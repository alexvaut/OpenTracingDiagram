FROM grafana/grafana:6.1.6
RUN grafana-cli plugins install jdbranham-diagram-panel \
    && grafana-cli plugins install simpod-json-datasource
COPY --chown=grafana:grafana ./provisioning /etc/grafana/provisioning
