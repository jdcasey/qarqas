package org.commonjava.maven.plugins.arqas.conf;

import static org.codehaus.plexus.util.FileUtils.fileRead;
import static org.codehaus.plexus.util.IOUtil.close;
import static org.codehaus.plexus.util.IOUtil.copy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.commonjava.qarqas.registry.model.PortConfiguration;
import org.commonjava.qarqas.registry.model.PortConfigurationBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ReservationPortConfiguratorTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void substitutePortsCorrectly()
        throws Exception
    {
        final PortConfiguration ports = new PortConfigurationBuilder().key( 1 )
                                                                      .port( "http", 10080 )
                                                                      .build();
        final File standaloneXml = getStandaloneXml();
        new TestReservationPortConfigurator().rewriteConfig( standaloneXml,
                                                             new AbstractPortConfigurator.StandaloneSocketsResolver(),
                                                             ports, new SystemStreamLog() );

        final String result = fileRead( standaloneXml );
        assertThat( result.contains( "10080" ), equalTo( true ) );
    }

    private File getStandaloneXml()
        throws Exception
    {
        final InputStream resource = Thread.currentThread()
                                           .getContextClassLoader()
                                           .getResourceAsStream( "standalone.xml" );
        final File xml = temp.newFile( "standalone.xml" );

        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream( xml );
            copy( resource, fos );
        }
        finally
        {
            close( resource );
            close( fos );
        }

        return xml;
    }

    private static final class TestReservationPortConfigurator
        extends ReservationPortConfigurator
    {

    }

}
