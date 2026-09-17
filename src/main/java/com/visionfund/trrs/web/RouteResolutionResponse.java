package com.visionfund.trrs.web;

public record RouteResolutionResponse(
        Integer tenantId,
        String sourceSystemCode,
        String destinationSystemCode,
        String authMethod,
        String secretsPath,
        String cbsAdapter,
        String endpoints,
        String s3BucketUrl,
        String s3BucketName,
        String dwEndpoint,
        String dwDatabaseName) {
}
