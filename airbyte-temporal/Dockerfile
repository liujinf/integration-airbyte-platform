# A test describe in the README is available to test a version update
FROM temporalio/auto-setup:1.22.7

ENV TEMPORAL_HOME /etc/temporal

COPY update-and-start-temporal.sh update-and-start-temporal.sh

ENTRYPOINT ["./update-and-start-temporal.sh"]
