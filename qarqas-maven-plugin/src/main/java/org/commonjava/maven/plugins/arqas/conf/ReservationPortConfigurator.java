package org.commonjava.maven.plugins.arqas.conf;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.List;
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
import org.cdmckay.coffeedom.Attribute;
import org.cdmckay.coffeedom.Document;
import org.cdmckay.coffeedom.Element;
import org.cdmckay.coffeedom.input.SAXBuilder;
import org.cdmckay.coffeedom.output.Format;
import org.cdmckay.coffeedom.output.XMLOutputter;
import org.cdmckay.coffeedom.xpath.XPath;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.IOUtil;
import org.commonjava.qarqas.registry.model.PortConfiguration;

import com.google.gson.GsonBuilder;

@Component( role = ASConfigurator.class, hint = "reservation" )
public class ReservationPortConfigurator
    implements ASConfigurator
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

        final File standaloneXml = new File( jbossasDir, "standalone/configuration/standalone.xml" );
        Document doc;
        try
        {
            doc = new SAXBuilder().build( standaloneXml );
        }
        catch ( final IOException e )
        {
            throw new MojoExecutionException( "Cannot read standalone.xml file: " + e.getMessage(), e );
        }

        final List<?> nodes = XPath.selectNodes( doc, "//socket-binding-group/*[@port]" );
        boolean changed = false;
        for ( final Object nodeObj : nodes )
        {
            final Element elem = (Element) nodeObj;
            final Attribute attr = elem.getAttribute( "port" );
            if ( attr != null )
            {
                final String portVal = attr.getValue();

                final Integer port = portConfig.getPort( elem.getName() );
                if ( port != null )
                {
                    attr.setValue( Integer.toString( port ) );
                    changed = changed || !portVal.equals( attr.getValue() );
                }
            }
        }

        if ( !changed )
        {
            return;
        }

        final Format format = Format.getRawFormat();
        final XMLOutputter outputter = new XMLOutputter( format );
        final String xml = outputter.outputString( doc );

        FileWriter writer = null;
        try
        {
            writer = new FileWriter( standaloneXml );
            writer.write( xml );
        }
        catch ( final IOException e )
        {
            throw new MojoExecutionException( "Cannot write standalone.xml file: " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    private PortConfiguration reservePorts( final Properties config, final Log log )
        throws MojoExecutionException
    {
        final String clientKey = config.getProperty( CLIENT_KEY_CONFIG );
        final String baseUrl = config.getProperty( RESERVATION_BASE_URL, DEFAULT_BASE_URL );
        final String u = baseUrl + clientKey;
        log.info( "Releasing port configuration reservation via: " + u );

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
