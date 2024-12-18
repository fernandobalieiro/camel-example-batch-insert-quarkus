package com.acme;

import java.util.Iterator;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.zipfile.ZipFileDataFormat;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.apache.camel.model.dataformat.BindyType.Csv;

@ApplicationScoped
@RequiredArgsConstructor
public class MyRouteBuilder extends RouteBuilder {

    private final MapAggregationStrategy mapAggregationStrategy;

    public void configure() {
        final var zipFile = new ZipFileDataFormat();
        zipFile.setUsingIterator(true);

        // Configure Camel Context to use MDC Logging. This may degrade performance, use with caution.
//        getContext().setUseMDCLogging(true);
//        getContext().setUseBreadcrumb(true);

        from("file:src/data?noop=true&initialDelay=0&antInclude=*.zip").id("fromDir")
            .log(">>> Start processing zip file: ${header.CamelFileName}")
            .onCompletion()
                .log("<<< Finished processing file: ${header.CamelFileName}")
            .end()
            .unmarshal(zipFile)
            .split(bodyAs(Iterator.class)).streaming()
            .log(">>> Start processing file: ${header.CamelFileName}")
            .split().tokenize("\n", 1, true)
                .streaming().parallelProcessing()
            .unmarshal().bindy(Csv, Person.class)
            .aggregate(mapAggregationStrategy)
                .constant(true)
                .completionSize(1000)
                .completionTimeout(500)
            .parallelProcessing()
            .log(DEBUG, "Aggregated ${body.size()} records")
            .to("sql:insert into people (name, age) values (:#name, :#age)?batch=true").id("toSql")
            .log(DEBUG, "Inserted ${body.size()} records into the database")
        ;
    }

}
