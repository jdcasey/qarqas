package org.commonjava.maven.plugins.arqas.conf;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.cdmckay.coffeedom.Attribute;
import org.cdmckay.coffeedom.Document;
import org.cdmckay.coffeedom.Element;
import org.cdmckay.coffeedom.Namespace;
import org.cdmckay.coffeedom.input.SAXBuilder;
import org.cdmckay.coffeedom.output.Format;
import org.cdmckay.coffeedom.output.XMLOutputter;
import org.codehaus.plexus.util.IOUtil;
import org.commonjava.qarqas.registry.model.PortConfiguration;

public abstract class AbstractPortConfigurator
    implements ASConfigurator
{

    protected void rewriteDomainXml( final File jbossasDir, final PortConfiguration portConfig, final Log log )
        throws MojoExecutionException
    {
        final File domainXml = new File( jbossasDir, "domain/configuration/domain.xml" );
        rewriteConfig( domainXml, portConfig, log );
    }

    protected void rewriteStandaloneXml( final File jbossasDir, final PortConfiguration portConfig, final Log log )
        throws MojoExecutionException
    {
        final File standaloneXml = new File( jbossasDir, "standalone/configuration/standalone.xml" );
        rewriteConfig( standaloneXml, portConfig, log );
    }

    protected void rewriteConfig( final File xml, final PortConfiguration portConfig, final Log log )
        throws MojoExecutionException
    {
        log.info( "Parsing XML from: " + xml );
        Document doc;
        try
        {
            doc = new SAXBuilder().build( xml );
        }
        catch ( final IOException e )
        {
            throw new MojoExecutionException( "Cannot read file: " + e.getMessage(), e );
        }

        final Namespace ns = doc.getRootElement()
                                .getNamespace();

        final Element sockets = doc.getRootElement()
                                   .getChild( "socket-binding-group", ns );
        boolean changed = false;
        if ( sockets != null )
        {
            final List<Element> children = sockets.getChildren();
            log.info( "Found " + children.size() + " socket specifications..." );
            for ( final Element child : children )
            {
                final String name = child.getAttribute( "name" )
                                         .getValue();

                log.info( "Attempting to configure port for: " + name );

                final Attribute attr = child.getAttribute( "port" );
                if ( attr != null )
                {
                    final String portVal = attr.getValue();

                    final Integer port = portConfig.getPort( name );
                    if ( port != null )
                    {
                        attr.setValue( Integer.toString( port ) );
                        changed = changed || !portVal.equals( attr.getValue() );
                        log.info( "Reservation-based configuration set port named: " + child.getName() + " to value: "
                            + attr.getValue() );
                    }
                }
            }
        }

        if ( !changed )
        {
            log.warn( "Reservation-based configuration produced NO changes! Not writing to disk." );
            return;
        }

        log.info( "Writing XML to: " + xml );
        final Format format = Format.getRawFormat();
        final XMLOutputter outputter = new XMLOutputter( format );
        final String xmlStr = outputter.outputString( doc );

        FileWriter writer = null;
        try
        {
            writer = new FileWriter( xml );
            writer.write( xmlStr );
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
