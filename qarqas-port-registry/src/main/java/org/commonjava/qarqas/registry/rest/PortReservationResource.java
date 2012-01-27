package org.commonjava.qarqas.registry.rest;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.qarqas.registry.data.PortDataException;
import org.commonjava.qarqas.registry.data.PortDataManager;
import org.commonjava.qarqas.registry.model.PortConfiguration;
import org.commonjava.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Singleton
@Path( "/reservation" )
public class PortReservationResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private PortDataManager data;

    @GET
    @Path( "/{clientId}" )
    @Produces( "application/json" )
    public Response reserve( @PathParam( "clientId" ) final String clientKey,
                             @QueryParam( "expires" ) final Long expiration )
    {
        if ( clientKey == null )
        {
            return Response.status( Status.BAD_REQUEST )
                           .build();
        }

        try
        {
            PortConfiguration reservation = data.getReservation( clientKey );

            // use the current reservation for this clientKey, if one exists.
            if ( reservation == null )
            {
                reservation = data.reserve( clientKey, expiration );
            }
            else
            {
                data.renew( reservation, expiration );
            }

            if ( reservation != null )
            {
                return Response.ok( toJson( reservation ) )
                               .build();
            }

            return Response.status( Status.SERVICE_UNAVAILABLE )
                           .build();
        }
        catch ( final PortDataException e )
        {
            logger.error( "Failed to reserve: %s", e, e.getMessage() );
            return Response.serverError()
                           .build();
        }

    }

    @DELETE
    @Path( "/{clientId}" )
    public Response release( @PathParam( "clientId" ) final String clientKey )
    {
        if ( clientKey == null )
        {
            return Response.status( Status.BAD_REQUEST )
                           .build();
        }

        try
        {
            final PortConfiguration reservation = data.getReservation( clientKey );
            if ( reservation == null )
            {
                return Response.ok( "No configuration was reserved, or reservation had already expired." )
                               .build();
            }

            final Integer key = reservation.getKey();
            data.release( key, clientKey );
            return Response.ok( "Configuration " + key + " was released." )
                           .build();
        }
        catch ( final PortDataException e )
        {
            logger.error( "Failed to release port-configuration reservation for: %s. Reason: %s", e, clientKey,
                          e.getMessage() );
            return Response.serverError()
                           .build();
        }
    }

    @POST
    @Path( "/ban/{key}" )
    public Response ban( @PathParam( "key" ) final Integer key )
    {
        try
        {
            data.ban( key );
            return Response.ok( "Configuration " + key + " was banned." )
                           .build();
        }
        catch ( final PortDataException e )
        {
            logger.error( "Failed to ban ports in reservation: %d. Reason: %s", e, key, e.getMessage() );
            return Response.serverError()
                           .build();
        }
    }

    private String toJson( final PortConfiguration reservation )
    {
        return getGson().toJson( reservation );
    }

    private Gson getGson()
    {
        return new GsonBuilder().setPrettyPrinting()
                                .create();
    }

}
