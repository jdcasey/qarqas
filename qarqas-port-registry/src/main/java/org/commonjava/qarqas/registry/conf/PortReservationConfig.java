package org.commonjava.qarqas.registry.conf;

import javax.inject.Singleton;

@Singleton
public class PortReservationConfig
{

    private long leasePeriod = 5 * 60 * 1000; // 5 mins.

    long getLeasePeriod()
    {
        return leasePeriod;
    }

    void setLeasePeriod( final long leasePeriod )
    {
        this.leasePeriod = leasePeriod;
    }

}
