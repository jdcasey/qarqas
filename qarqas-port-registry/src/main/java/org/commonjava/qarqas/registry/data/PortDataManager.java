package org.commonjava.qarqas.registry.data;

import org.commonjava.qarqas.registry.model.PortConfiguration;

public interface PortDataManager
{

    void initialize()
        throws PortDataException;

    void destroy();

    PortConfiguration getReservation( String clientKey )
        throws PortDataException;

    PortConfiguration reserve( String clientKey, Long expiration )
        throws PortDataException;

    void renew( PortConfiguration reservation, Long expiration )
        throws PortDataException;

    void release( PortConfiguration reservation, String clientKey )
        throws PortDataException;

    void release( Integer reservationKey, String clientKey )
        throws PortDataException;

    void ban( PortConfiguration reservation )
        throws PortDataException;

    void ban( Integer reservationKey )
        throws PortDataException;

    void clearExpiredReservations();

}