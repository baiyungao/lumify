package io.lumify.core.model.longRunningProcess;

import com.google.inject.Inject;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.web.clientapi.model.ClientApiElement;
import io.lumify.web.clientapi.model.ClientApiVertex;
import io.lumify.web.clientapi.model.ClientApiVertexFindPathResponse;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Path;
import org.securegraph.Vertex;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class FindPathLongRunningProcessWorker extends LongRunningProcessWorker {
    private Graph graph;
    private AuthorizationRepository authorizationRepository;

    @Override
    public boolean isHandled(JSONObject longRunningProcessQueueItem) {
        return longRunningProcessQueueItem.getString("type").equals("findPath");
    }

    @Override
    public void process(JSONObject longRunningProcessQueueItem) {
        FindPathLongRunningProcessQueueItem findPath = ClientApiConverter.toClientApi(longRunningProcessQueueItem.toString(), FindPathLongRunningProcessQueueItem.class);

        Authorizations authorizations = getAuthorizations(findPath.getAuthorizations());
        Vertex sourceVertex = this.graph.getVertex(findPath.getSourceVertexId(), authorizations);
        checkNotNull(sourceVertex, "Could not find source vertex: " + findPath.getSourceVertexId());
        Vertex destVertex = this.graph.getVertex(findPath.getDestVertexId(), authorizations);
        checkNotNull(destVertex, "Could not find destination vertex: " + findPath.getDestVertexId());
        int hops = findPath.getHops();
        String workspaceId = findPath.getWorkspaceId();

        ClientApiVertexFindPathResponse results = new ClientApiVertexFindPathResponse();
        Iterable<Path> paths = graph.findPaths(sourceVertex, destVertex, hops, authorizations);
        for (Path path : paths) {
            List<ClientApiElement> clientApiElementPath = ClientApiConverter.toClientApi(graph.getVerticesInOrder(path, authorizations), workspaceId, authorizations);
            List<ClientApiVertex> clientApiVertexPath = new ArrayList<ClientApiVertex>();
            for (ClientApiElement e : clientApiElementPath) {
                clientApiVertexPath.add((ClientApiVertex) e);
            }
            results.getPaths().add(clientApiVertexPath);
        }

        String resultsString = ClientApiConverter.clientApiToString(results);
        JSONObject resultsJson = new JSONObject(resultsString);
        longRunningProcessQueueItem.put("results", resultsJson);
    }

    private Authorizations getAuthorizations(String[] authorizations) {
        return authorizationRepository.createAuthorizations(authorizations);
    }

    @Inject
    public void setAuthorizationRepository(AuthorizationRepository authorizationRepository) {
        this.authorizationRepository = authorizationRepository;
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
    }
}
