package org.commonjava.maven.plugins.arqas;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.commonjava.maven.plugins.arqas.conf.ASConfigurator;

/**
 * Remove the JBossAS distribution configured for testing this project.
 * 
 * @goal teardown
 * @phase post-integraion-test
 * @author jdcasey
 */
public class TeardownArqASGoal
    extends AbstractArqASGoal
{

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        final File dir = getASDir();

        final Properties props = createConfiguratorProperties();
        for ( final ASConfigurator configurator : eachConfigurator() )
        {
            configurator.cleanup( dir, props, getLog() );
        }

        if ( dir.exists() )
        {
            try
            {
                FileUtils.forceDelete( dir );
            }
            catch ( final IOException e )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().warn( "Failed to delete JBossAS directory: " + dir + ". Reason: " + e.getMessage(), e );
                }
                else
                {
                    getLog().warn( "Failed to delete JBossAS directory: " + dir + ". Reason: " + e.getMessage() );
                }
            }
        }
    }

}
