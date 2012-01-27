package org.commonjava.maven.plugins.arqas.conf;

import static org.commonjava.qarqas.registry.model.PortConfiguration.STANDARD;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.annotations.Component;
import org.commonjava.qarqas.registry.model.PortConfiguration;

@Component( role = ASConfigurator.class, hint = "port-shift" )
public class SimplePortShiftConfigurator
    extends AbstractPortConfigurator
{

    public static final String PORT_SHIFT_CONFIG = "portShift";

    @Override
    protected PortConfiguration getPortConfiguration( final File jbossasDir, final Properties config, final Log log )
        throws MojoExecutionException
    {
        final PortConfiguration portConfig = new PortConfiguration( STANDARD );
        final String shiftVal = config.getProperty( PORT_SHIFT_CONFIG );
        if ( shiftVal == null )
        {
            return null;
        }

        final int shift = Integer.parseInt( shiftVal );
        for ( final Map.Entry<String, Integer> entry : portConfig )
        {
            portConfig.setPort( entry.getKey(), shift + entry.getValue() );
        }
        return portConfig;
    }

}
