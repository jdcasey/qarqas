package org.commonjava.maven.plugins.arqas;

import java.io.IOException;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.commonjava.maven.plugins.arqas.conf.ASConfigurator;

/**
 * Remove the JBossAS distribution configured for testing this project.
 * 
 * @goal clean
 * @author jdcasey
 */
public class CleanArqASGoal
    extends AbstractArqASGoal
{

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        final Properties props = createConfiguratorProperties();
        for ( final ASConfigurator configurator : eachConfigurator() )
        {
            configurator.cleanup( output, props, getLog() );
        }

        if ( output.exists() )
        {
            try
            {
                FileUtils.forceDelete( output );
            }
            catch ( final IOException e )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().warn( "Failed to delete JBossAS directory: " + output + ". Reason: " + e.getMessage(), e );
                }
                else
                {
                    getLog().warn( "Failed to delete JBossAS directory: " + output + ". Reason: " + e.getMessage() );
                }
            }
        }
    }

}
