package org.commonjava.maven.plugins.arqas;

import static org.codehaus.plexus.util.IOUtil.close;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.commonjava.maven.plugins.arqas.conf.ASConfigurator;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.impl.ArtifactResolver;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * Download the JBossAS distribution, unpack it, and setup an arquillian.xml file that points to this location (for
 * testing in the context of other running builds).
 * 
 * @goal setup
 * @phase pre-integration-test
 * @author jdcasey
 */
public class SetupArqASGoal
    extends AbstractArqASGoal
{

    private static final String JBOSS_AS_PATH = "$JBOSS_HOME";

    public static final String MGMT_PORT = "$MGMT_PORT";

    public static final String DEFAULT_MGMT_PORT = "9999";

    /**
     * File location (default: target/test-classes/arquillian.xml) where the generated ARQ configuration file will be
     * written.
     * 
     * @parameter default-value="${project.build.testOutputDirectory}/arquillian.xml"
     *            expression="${qarqas.arquillianXml}"
     */
    private File arquillianXml;

    /**
     * Classpath resource (default: arquillian.xml) which will be used as the template for generating the ARQ
     * configuration file pointing at the unpacked JBossAS distribution.
     * 
     * @parameter default-value="arquillian.xml" expression="${qarqas.arquillianXmlResource}"
     */
    private String arquillianXmlResource;

    /**
     * @parameter default-value="${session}"
     * @readonly
     */
    private MavenSession session;

    /**
     * @parameter default-value="${project.remoteArtifactRepositories}"
     * @readonly
     */
    private List<ArtifactRepository> repos;

    /**
     * @component
     */
    private ArtifactResolver resolver;

    /**
     * @component role-hint="zip"
     */
    private UnArchiver zipUnarchiver;

    /**
     * If true, delete any pre-existing files in the destination directory and then re-extract the JBossAS distribution.
     * 
     * @parameter default-value="false" expression="${arqas.overwrite}"
     */
    private boolean overwrite;

    /**
     * Port configurations and other properties specific to the configured AS7 instance are saved here for tests to
     * lookup.
     * 
     * @parameter default-value="${project.build.testOutputDirectory}/qarqas.properties" expression="${qarqas.exports}"
     */
    private File exportProperties;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        boolean doUnpack = true;
        if ( output.exists() )
        {
            if ( overwrite )
            {
                try
                {
                    FileUtils.deleteDirectory( output );
                }
                catch ( final IOException e )
                {
                    throw new MojoExecutionException( "Failed to delete pre-existing output directory: " + output, e );
                }
            }
            else
            {
                getLog().info( "Distribution directory: "
                                   + output
                                   + " already exists. To overwrite, set the 'overwrite' parameter to true (CLI: -Darqas.overwrite=true)." );
                doUnpack = false;
            }
        }

        final Properties props = createConfiguratorProperties();

        if ( doUnpack )
        {
            resolveAndUnpack();
        }

        final Map<String, String> exports = runConfigurators( props );
        generateArqXml( props );

        if ( exports != null )
        {
            final Properties p = new Properties();
            p.putAll( exports );

            final StringWriter sw = new StringWriter();
            props.list( new PrintWriter( sw ) );
            getLog().info( "Exporting:\n\n" + sw.toString() );

            FileOutputStream fos = null;
            try
            {
                fos = new FileOutputStream( exportProperties );
                p.store( fos, "generated by QARQAS at: " + new Date() );
            }
            catch ( final IOException e )
            {
                throw new MojoExecutionException( "Failed to store exported properties in: " + exportProperties
                    + ". Reason: " + e.getMessage(), e );
            }
            finally
            {
                close( fos );
            }
        }
    }

    private void generateArqXml( final Properties props )
        throws MojoExecutionException
    {
        final InputStream stream = Thread.currentThread()
                                         .getContextClassLoader()
                                         .getResourceAsStream( arquillianXmlResource );
        if ( stream == null )
        {
            throw new MojoExecutionException( "Cannot read arquillian.xml source resource from classpath: '"
                + arquillianXmlResource + "'." );
        }

        String arqXml;
        try
        {
            arqXml = IOUtil.toString( stream );
        }
        catch ( final IOException e )
        {
            throw new MojoExecutionException( "Cannot read arquillian.xml source resource from classpath: '"
                + arquillianXmlResource + "'. Reason: " + e.getMessage(), e );
        }

        final String portStr = props.getProperty( MGMT_PORT, DEFAULT_MGMT_PORT );
        arqXml = arqXml.replace( MGMT_PORT, portStr );
        arqXml = arqXml.replace( JBOSS_AS_PATH, getASDir().getAbsolutePath() );

        FileWriter writer = null;
        try
        {
            final File dir = arquillianXml.getParentFile();
            if ( !dir.exists() )
            {
                dir.mkdirs();
            }

            writer = new FileWriter( arquillianXml );
            writer.write( arqXml );
        }
        catch ( final IOException e )
        {
            throw new MojoExecutionException( "Failed to write ARQ configuration to: '" + arquillianXml + "'. Reason: "
                + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    private void resolveAndUnpack()
        throws MojoExecutionException
    {
        final String[] coord = asCoordinate.split( ":" );
        final String g = coord[0];
        final String a = coord[1];
        final String v = coord[2];
        final String type = "zip";

        final Artifact artifact = new DefaultArtifact( g, a, type, v );

        final List<RemoteRepository> remoteRepos = RepositoryUtils.toRepos( repos );
        final ArtifactRequest req = new ArtifactRequest( artifact, remoteRepos, "plugin" );

        File zip;
        try
        {
            final ArtifactResult resolved = resolver.resolveArtifact( session.getRepositorySession(), req );
            zip = resolved.getArtifact()
                          .getFile();
        }
        catch ( final ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Failed to resolve JBossAS ZIP archive: '" + asCoordinate + "'. Reason: "
                + e.getMessage(), e );
        }

        output.mkdirs();
        zipUnarchiver.setDestDirectory( output );
        zipUnarchiver.setSourceFile( zip );
        zipUnarchiver.extract();
    }

    private Map<String, String> runConfigurators( final Properties props )
        throws MojoExecutionException
    {
        final Map<String, String> exports = new HashMap<String, String>();
        for ( final ASConfigurator configurator : eachConfigurator() )
        {
            exports.putAll( configureWith( configurator, props ) );
        }

        return exports;
    }

    private Map<String, String> configureWith( final ASConfigurator configurator, final Properties props )
        throws MojoExecutionException
    {
        final File dir = getASDir();
        return configurator.configure( dir, props, getLog() );
    }

}
