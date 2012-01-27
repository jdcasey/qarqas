package org.commonjava.maven.plugins.arqas.conf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Properties;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.IOUtil;
import org.commonjava.qarqas.registry.model.PortConfiguration;

import com.google.gson.GsonBuilder;

@Component( role = ASConfigurator.class, hint = "reservation" )
public class ReservationPortConfigurator
    extends AbstractPortConfigurator
{

    public static final String CLIENT_KEY_CONFIG = "clientKey";

    public static final String RESERVATION_BASE_URL = "reservationBaseUrl";

    public static final String DEFAULT_BASE_URL = "http://127.0.0.1:9080/qarqas/api/1.0/reservation/";

    @Override
    public void configure( final File jbossasDir, final Properties config, final Log log )
        throws MojoExecutionException
    {
        final PortConfiguration portConfig = reservePorts( config, log );
        log.info( "Using AS port configuration:\n\n" + portConfig );

        rewriteDomainXml( jbossasDir, portConfig, log );
        rewriteStandaloneXml( jbossasDir, portConfig, log );
    }

    private PortConfiguration reservePorts( final Properties config, final Log log )
        throws MojoExecutionException
    {
        final String clientKey = config.getProperty( CLIENT_KEY_CONFIG );
        final String baseUrl = config.getProperty( RESERVATION_BASE_URL, DEFAULT_BASE_URL );
        final String u = baseUrl + clientKey;
        log.info( "Reserving port configuration reservation via: " + u );

        InputStream stream = null;
        try
        {
            final HttpClient client = new DefaultHttpClient();
            final HttpGet req = new HttpGet( u );
            req.setHeader( HttpHeaders.ACCEPT, "application/json" );
            final HttpResponse response = client.execute( req );

            final StatusLine statusLine = response.getStatusLine();
            if ( statusLine.getStatusCode() == HttpStatus.SC_OK )
            {
                stream = response.getEntity()
                                 .getContent();
                return new GsonBuilder().create()
                                        .fromJson( new InputStreamReader( stream ), PortConfiguration.class );
            }

            throw new MojoExecutionException(
                                              "Cannot reserve port configuration from registry. Received HTTP response: "
                                                  + statusLine );
        }
        catch ( final MalformedURLException e )
        {
            throw new MojoExecutionException( "Cannot format reservation URL. clientKey appears to be invalid: "
                + clientKey + ". Reason: " + e.getMessage(), e );
        }
        catch ( final IOException e )
        {
            throw new MojoExecutionException( "Cannot contact port configuration registry at: " + u + ". Reason: "
                + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( stream );
        }
    }

    @Override
    public void cleanup( final File jbossasDir, final Properties config, final Log log )
    {
        final String clientKey = config.getProperty( CLIENT_KEY_CONFIG );
        final String baseUrl = config.getProperty( RESERVATION_BASE_URL, DEFAULT_BASE_URL );
        final String u = baseUrl + clientKey;
        log.info( "Releasing port configuration reservation via: " + u );

        try
        {
            final HttpClient client = new DefaultHttpClient();
            final HttpDelete req = new HttpDelete( u );
            final HttpResponse response = client.execute( req );

            final StatusLine statusLine = response.getStatusLine();
            if ( statusLine.getStatusCode() != HttpStatus.SC_OK )
            {
                log.error( "Cannot release port configuration from registry. Received HTTP response: " + statusLine );
            }
        }
        catch ( final MalformedURLException e )
        {
            log.error( "Cannot format reservation URL. clientKey appears to be invalid: " + clientKey + ". Reason: "
                + e.getMessage(), e );
        }
        catch ( final IOException e )
        {
            log.error( "Cannot contact port configuration registry at: " + u + ". Reason: " + e.getMessage(), e );
        }
    }

}
