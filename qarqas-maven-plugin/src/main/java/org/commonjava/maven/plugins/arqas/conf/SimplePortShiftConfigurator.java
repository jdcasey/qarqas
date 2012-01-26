package org.commonjava.maven.plugins.arqas.conf;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.cdmckay.coffeedom.Attribute;
import org.cdmckay.coffeedom.Document;
import org.cdmckay.coffeedom.Element;
import org.cdmckay.coffeedom.input.SAXBuilder;
import org.cdmckay.coffeedom.output.Format;
import org.cdmckay.coffeedom.output.XMLOutputter;
import org.cdmckay.coffeedom.xpath.XPath;
import org.codehaus.plexus.util.IOUtil;

public class SimplePortShiftConfigurator
    implements ASConfigurator
{

    public static final String PORT_SHIFT_CONFIG = "portShift";

    @Override
    public void configure( final File jbossasDir, final Properties config, final Log log )
        throws MojoExecutionException
    {
        final String shiftVal = config.getProperty( PORT_SHIFT_CONFIG );
        if ( shiftVal == null )
        {
            return;
        }

        final int shift = Integer.parseInt( shiftVal );
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
        for ( final Object nodeObj : nodes )
        {
            final Element elem = (Element) nodeObj;
            final Attribute attr = elem.getAttribute( "port" );
            if ( attr != null )
            {
                int port = Integer.parseInt( attr.getValue() );
                port += shift;
                attr.setValue( Integer.toString( port ) );
            }
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

}
