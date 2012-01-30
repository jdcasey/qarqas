package org.commonjava.maven.plugins.arqas;

import static org.commonjava.maven.plugins.arqas.QArqASConstants.ARQ_AS_CONFIG_PREFIX;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.commonjava.maven.plugins.arqas.conf.ASConfigurator;
import org.commonjava.maven.plugins.arqas.conf.ReservationPortConfigurator;

public abstract class AbstractArqASGoal
    implements Mojo
{

    private static final Set<String> DEFAULT_CONFIGURATORS = new HashSet<String>()
    {
        private static final long serialVersionUID = 1L;

        {
            add( "port-shift" );
        };
    };

    /**
     * GAV (GroupId:ArtifactId:Version) for JBossAS distribution to resolve and unpack.
     * 
     * @parameter default-value="org.jboss.as:jboss-as-dist:7.1.0.CR1b" expression="${qarqas.coordinate}"
     */
    protected String asCoordinate;

    /**
     * Name of root directory within JBossAS distribution zip. Default is calculated as 'jboss-as-${VERSION}', using the
     * version from the asCoordinate parameter.
     * 
     * @parameter expression="${qarqas.dirName}"
     */
    protected String asDirName;

    /**
     * File location (default: target/jbossas) where resolved JBossAS distribution should be unpacked.
     * 
     * @parameter default-value="${project.build.directory}/jbossas" expression="${qarqas.output}"
     */
    protected File output;

    /**
     * Key used to retrieve a reservation for a port configuration, if that approach is used.
     * 
     * @parameter default-value="${project.artifactId}" expression="${qarqas.clientKey}"
     */
    protected String clientKey;

    /**
     * Comma-separated list of {@link ASConfigurator} implementations to apply to resolved JBossAS distribution before
     * use.
     * 
     * @parameter expression="${qarqas.configurators}"
     */
    private String configurators;

    /**
     * Whether to apply the default list of {@link ASConfigurator} implementations.
     * 
     * @parameter expression="${qarqas.useDefaultConfigurators}"
     */
    private Boolean useDefaultConfigurators;

    /**
     * Properties to be passed into the listed configurators, to transform the JBossAS distribution before use.
     * (<b>NOTE:</b> Using -Dqarqas.config.FOO=BAR is also allowed, which enables command-line configuration.)
     * 
     * @parameter
     */
    private Map<String, String> configProperties;

    /**
     * @component role="org.commonjava.maven.plugins.arqas.conf.ASConfigurator"
     */
    private Map<String, ASConfigurator> configuratorMap;

    private Log log;

    protected final Properties createConfiguratorProperties()
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

        props.setProperty( ReservationPortConfigurator.CLIENT_KEY_CONFIG, clientKey );

        if ( configProperties != null )
        {
            props.putAll( configProperties );
        }

        return props;
    }

    protected File getASDir()
    {
        String dirname = asDirName;
        if ( dirname == null )
        {
            dirname = "jboss-as-" + getASVersion();
        }

        return new File( output, dirname );
    }

    protected String getASVersion()
    {
        return asCoordinate.split( ":" )[2];
    }

    @Override
    public final synchronized Log getLog()
    {
        if ( log == null )
        {
            log = new SystemStreamLog();
        }

        return log;
    }

    @Override
    public final void setLog( final Log log )
    {
        this.log = log;
    }

    protected final Iterable<ASConfigurator> eachConfigurator()
    {
        final LinkedHashSet<ASConfigurator> result = new LinkedHashSet<ASConfigurator>();
        if ( ( useDefaultConfigurators != null && useDefaultConfigurators )
            || ( useDefaultConfigurators == null && this.configurators == null ) )
        {
            for ( final String hint : DEFAULT_CONFIGURATORS )
            {
                result.add( getConfigurator( hint ) );
            }
        }

        if ( this.configurators != null )
        {
            for ( final String hint : this.configurators.split( "\\s*,\\s*" ) )
            {
                result.add( getConfigurator( hint ) );
            }
        }

        return result;
    }

    protected ASConfigurator getConfigurator( final String hint )
    {
        final ASConfigurator configurator = configuratorMap.get( hint );
        if ( configurator == null )
        {
            getLog().warn( "Cannot find ASConfigurator with hint: '" + hint + "'." );
        }
        return configurator;
    }

}
