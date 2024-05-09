package au.com.cfs.winged.core.services;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Fund-Card Components Service ",
        description = "Configure services for CFS component services")
public @interface FundCardComponentConfiguration {

    @AttributeDefinition(name = "Domain Name", description = "Domain Name for the API GateWay e.g: 'https://secure.colonialfirststate.com.au' ", type = AttributeType.STRING )
    String apiGatewayDomain();

    @AttributeDefinition(name = "Price & Performance API path", description = "API path for the Price & Performance Values Path e.g: '/fp/pricenperformance/products/funds/performance?' ", type = AttributeType.STRING )
    String performanceValuesApiPath();

    @AttributeDefinition(name = "connectTimeOutValue", description = "Connect Time Out (in milliseconds)",  type = AttributeType.INTEGER )
    int connectTimeOutValue();

    @AttributeDefinition(name = "readTimeOutValue", description = "Read Time Out (in milliseconds)",  type = AttributeType.INTEGER )
    int readTimeOutValue();


}
