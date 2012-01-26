package org.commonjava.maven.plugins.arqas;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.commonjava.maven.plugins.arqas.conf.ASConfigurator;
import org.commonjava.maven.plugins.arqas.conf.SimplePortShiftConfigurator;
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
 * @goal setup-as
 * @author jdcasey
 */
public class SetupArqASGoal
    extends AbstractArqASGoal
{

    private static final Set<String> DEFAULT_CONFIGURATORS = new HashSet<String>()
    {
        private static final long serialVersionUID = 1L;

        {
            add( SimplePortShiftConfigurator.class.getSimpleName() );
        };
    };

    private static final String ARQ_AS_CONFIG_PREFIX = "arqas.config.";

    private static final String JBOSS_AS_PATH = "$JBOSS_HOME";

    /**
     * GAV (GroupId:ArtifactId:Version) for JBossAS distribution to resolve and unpack.
     * 
     * @parameter default-value="org.jboss.as:jboss-as-dist:7.1.0.CR1b" expression="${arqas.coordinate}"
     */
    private String asCoordinate;

    /**
     * File location (default: target/test-classes/arquillian.xml) where the generated ARQ configuration file will be
     * written.
     * 
     * @parameter default-value="${project.build.testOutputDirectory}/arquillian.xml"
     *            expression="${arqas.arquillianXml}"
     */
    private File arquillianXml;

    /**
     * Classpath resource (default: arquillian.xml) which will be used as the template for generating the ARQ
     * configuration file pointing at the unpacked JBossAS distribution.
     * 
     * @parameter default-value="arquillian.xml" expression="${arqas.arquillianXmlResource}"
     */
    private String arquillianXmlResource;

    /**
     * Properties to be passed into the listed configurators, to transform the JBossAS distribution before use.
     * (<b>NOTE:</b> Using -Darqas.config.FOO=BAR is also allowed, which enables command-line configuration.)
     * 
     * @parameter
     */
    private Map<String, String> configProperties;

    /**
     * Comma-separated list of {@link ASConfigurator} implementations to apply to resolved JBossAS distribution before
     * use.
     * 
     * @parameter expression="${arqas.configurators}"
     */
    private String configurators;

    /**
     * Whether to apply the default list of {@link ASConfigurator} implementations.
     * 
     * @parameter default-value="true" expression="${arqas.useDefaultConfigurators}"
     */
    private boolean useDefaultConfigurators;

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
     * @component role="org.commonjava.maven.plugins.arqas.ASConfigurator"
     */
    private Map<String, ASConfigurator> configuratorMap;

    /**
     * If true, delete any pre-existing files in the destination directory and then re-extract the JBossAS distribution.
     * 
     * @parameter default-value="false" expression="${arqas.overwrite}"
     */
    private boolean overwrite;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
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
                return;
            }
        }

        final Properties props = createConfiguratorProperties();

        resolveAndUnpack();

        runConfigurators( props );

        generateArqXml();
    }

    private void generateArqXml()
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

        arqXml = arqXml.replace( JBOSS_AS_PATH, output.getAbsolutePath() );

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

        zipUnarchiver.setDestDirectory( output );
        zipUnarchiver.setSourceFile( zip );
        zipUnarchiver.extract();
    }

    private void runConfigurators( final Properties props )
        throws MojoExecutionException
    {
        if ( useDefaultConfigurators )
        {
            for ( final String configurator : DEFAULT_CONFIGURATORS )
            {
                configureWith( configurator, props );
            }
        }

        if ( this.configurators != null )
        {
            final String[] configurators = this.configurators.split( "\\s*,\\s*" );
            for ( final String configurator : configurators )
            {
                configureWith( configurator, props );
            }
        }
    }

    private Properties createConfiguratorProperties()
    {
        final Properties props = new Properties();
        final Properties sysprops = System.getProperties();
        for ( final Object k : sysprops.keySet() )
        {
            final String key = (String) k;
            if ( key.startsWith( ARQ_AS_CONFIG_PREFIX ) )
            {
                props.setProperty( key.substring( ARQ_AS_CONFIG_PREFIX.length() ), sysprops.getProperty( key ) );
            }
        }

        if ( configProperties != null )
        {
            props.putAll( configProperties );
        }

        return props;
    }

    private void configureWith( final String configuratorHint, final Properties props )
        throws MojoExecutionException
    {
        final ASConfigurator configurator = configuratorMap.get( configuratorHint );
        if ( configurator == null )
        {
            getLog().warn( "Cannot find ASConfigurator with hint: '" + configuratorHint + "'. Skipping." );
        }

        configurator.configure( output, props, getLog() );
    }

}
