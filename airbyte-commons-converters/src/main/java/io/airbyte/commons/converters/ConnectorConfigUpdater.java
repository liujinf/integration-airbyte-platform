/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.commons.converters;

import com.google.common.hash.Hashing;
import datadog.trace.api.Trace;
import io.airbyte.api.client.AirbyteApiClient;
import io.airbyte.api.client.model.generated.DestinationIdRequestBody;
import io.airbyte.api.client.model.generated.DestinationRead;
import io.airbyte.api.client.model.generated.DestinationUpdate;
import io.airbyte.api.client.model.generated.SourceIdRequestBody;
import io.airbyte.api.client.model.generated.SourceRead;
import io.airbyte.api.client.model.generated.SourceUpdate;
import io.airbyte.commons.json.Jsons;
import io.airbyte.protocol.models.Config;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for workers to persist updates to Source/Destination configs emitted from
 * AirbyteControlMessages.
 *
 * This is in order to support connectors updating configs when running commands, which is specially
 * useful for migrating configuration to a new version or for enabling connectors that require
 * single-use or short-lived OAuth tokens.
 */
@Singleton
public class ConnectorConfigUpdater {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorConfigUpdater.class);

  private final AirbyteApiClient airbyteApiClient;

  public ConnectorConfigUpdater(final AirbyteApiClient airbyteApiClient) {
    this.airbyteApiClient = airbyteApiClient;
  }

  /**
   * Updates the Source from a sync job ID with the provided Configuration. Secrets and OAuth
   * parameters will be masked when saving.
   */
  @Trace
  public void updateSource(final UUID sourceId, final Config config) {
    final SourceRead source = AirbyteApiClient.retryWithJitter(
        () -> airbyteApiClient.getSourceApi().getSource(new SourceIdRequestBody().sourceId(sourceId)),
        "get source");

    final SourceRead updatedSource = AirbyteApiClient.retryWithJitter(
        () -> airbyteApiClient.getSourceApi()
            .updateSource(new SourceUpdate()
                .sourceId(sourceId)
                .name(source.getName())
                .connectionConfiguration(Jsons.jsonNode(config.getAdditionalProperties()))),
        "update source");

    LOGGER.info("Persisted updated configuration for source {}. New config hash: {}.", sourceId,
        Hashing.sha256().hashString(updatedSource.getConnectionConfiguration().asText(), StandardCharsets.UTF_8));

  }

  /**
   * Updates the Destination from a sync job ID with the provided Configuration. Secrets and OAuth
   * parameters will be masked when saving.
   */
  public void updateDestination(final UUID destinationId, final Config config) {
    final DestinationRead destination = AirbyteApiClient.retryWithJitter(
        () -> airbyteApiClient.getDestinationApi().getDestination(new DestinationIdRequestBody().destinationId(destinationId)),
        "get destination");

    final DestinationRead updatedDestination = AirbyteApiClient.retryWithJitter(
        () -> airbyteApiClient.getDestinationApi()
            .updateDestination(new DestinationUpdate()
                .destinationId(destinationId)
                .name(destination.getName())
                .connectionConfiguration(Jsons.jsonNode(config.getAdditionalProperties()))),
        "update destination");

    LOGGER.info("Persisted updated configuration for destination {}. New config hash: {}.", destinationId,
        Hashing.sha256().hashString(updatedDestination.getConnectionConfiguration().asText(), StandardCharsets.UTF_8));
  }

}
