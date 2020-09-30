#include <iostream>
#include <vector>
#include <algorithm>
#include <memory>
#include <queue>

using namespace std;

/*
 *  A class to store the edges of a graph and thier reverse for a corresponding
 *  residual graph.
 */
class FlowGraph
{
public:
    
    struct Edge
    {
        int from, to, capacity, flow;
        
        Edge(int from_, int to_, int capacity_, int flow_) :
        from(from_), to(to_), capacity(capacity_), flow(flow_) {}
        
        Edge(const Edge& e) :
        from(e.from), to(e.to), capacity(e.capacity), flow(e.flow) {}
    };
    
private:
    
    /* a list of edges and their reverse. */
    vector<Edge> edges;
    
    /* An adjacency list storing the outging edge indices for a node. */
    vector<vector<size_t> > graph;
    
public:
    
    explicit FlowGraph(size_t n): graph(n) {}
    
    void add_edge(int from, int to, int capacity)
    {
        /*  Append an edge so that it is stored at an even index and then
         append the corresponding reverse edge which sill then have an
         odd index.  */
        Edge forward_edge = {from, to, capacity, 0};
        Edge backward_edge = {to, from, 0, 0};
        
        graph[from].push_back(edges.size());
        edges.push_back(forward_edge);
        
        graph[to].push_back(edges.size());
        edges.push_back(backward_edge);
    }
    
    size_t size() const
    {
        return graph.size();
    }
    
    const vector<size_t>& get_ids(int from) const
    {
        return graph[from];
    }
    
    const Edge& get_edge(size_t id) const
    {
        return edges[id];
    }
    
    void add_flow(size_t id, int flow)
    {
        /*
         * To get a backward edge for a true forward edge (i.e id is even), we
         * should get id + 1 due to the described above scheme. On the other
         * hand, when we have to get a "backward" edge for a backward edge
         * (i.e. get a forward edge for backward - id is odd), id - 1 should
         * be taken.
         *
         * It turns out that id ^ 1 works for both cases. Think this through!
         */
        edges[id].flow += flow;
        edges[id ^ 1].flow -= flow;
    }
};

class MaxMatching
{
public:
    
    void Solve()
    {
        vector<vector<bool>> adj_matrix = ReadData();
        vector<int> matching = FindMatching(adj_matrix);
        WriteResponse(matching);
    }
    
private:
    
    vector<vector<bool>> ReadData()
    {
        int num_left, num_right;
        cin >> num_left >> num_right;
        vector<vector<bool>> adj_matrix(num_left, vector<bool>(num_right));
        
        for (int i = 0; i < num_left; ++i)
        {
            for (int j = 0; j < num_right; ++j)
            {
                int bit;
                cin >> bit;
                adj_matrix[i][j] = (bit == 1);
            }
        }
        return adj_matrix;
    }
    
    void WriteResponse(const vector<int>& matching)
    {
        for (int i = 0; i < matching.size(); ++i)
        {
            if (i > 0)
                cout << " ";
            if (matching[i] == -1)
                cout << "-1";
            else
                cout << (matching[i] + 1);
        }
        cout << "\n";
    }
    
    /* Convert a bipartite adjacency matrix to a network. */
    FlowGraph convertMatrix(const vector<vector<bool> >& adj_matrix)
    {
        /* if the matrix is n x m, then there will be n + m + 2 nodes. */
        int n = (int) adj_matrix.size();
        int m = (int) adj_matrix[0].size();
        FlowGraph graph(n + m + 2);
        
        int source = 0;
        int sink = n + m + 1;
        
        // connect the first n nodes to the source;
        for (int i = 1; i <= n; i++)
        {
            graph.add_edge(source, i, 1);
        }
        
        // parse the matrix and connect.
        for (int i = 0; i < n; ++i)
        {
            for (int j = 0; j < m; ++j)
            {
                if (adj_matrix[i][j])
                {
                    graph.add_edge(i + 1, j + n + 1, 1);
                }
            }
        }
        
        // connect the last m nodes to the sink.
        for (int j = n + 1; j < sink; ++j)
        {
            graph.add_edge(j, sink, 1);
        }
        
        return graph;
    }
    
    /* A BFS to find the shortest path on a graph. */
    vector<size_t> shortestPath(const FlowGraph& graph, int from, int to)
    {
        /* Return a list of vertex ids for the shortest path of from to to. */
        vector<size_t> path;
        vector<bool> found(graph.size(), false);
        vector<size_t> prev(graph.size());
        queue<int> q;
        
        // start at source and use BFS
        int current_node = from;
        found[current_node] = true;
        q.push(current_node);
        
        while (!q.empty() && !found[to])
        {
            current_node = q.front(); q.pop();
            
            vector<size_t> edge_ids = graph.get_ids(current_node);
            
            // check each edge from current node;
            for (vector<size_t>::iterator id = edge_ids.begin();
                 id != edge_ids.end(); ++id)
            {
                
                FlowGraph::Edge e = graph.get_edge(*id);
                
                if (!found[e.to] && e.flow < e.capacity)
                {
                    // add end node
                    q.push(e.to);
                    found[e.to] = true;
                    prev[e.to] = *id;
                    
                    if (e.to == to)
                        break;
                }
            }
        }
        
        if (found[to])  // a path exists
        {
            size_t node = to;
            
            while (node != from)
            {
                path.push_back(prev[node]);
                node = graph.get_edge(prev[node]).from;
            }
            
            std::reverse(path.begin(), path.end());
        }
        
        return path;
        
    }
    
    vector<int> FindMatching(const vector<vector<bool>>& adj_matrix)
    {
        int n = (int) adj_matrix.size();
        int m = (int) adj_matrix[0].size();
        vector<int> matching(n, -1);
        
        FlowGraph graph = convertMatrix(adj_matrix);
        int from = 0;
        int to = n + m + 1;
        vector<size_t> path = shortestPath(graph, from, to);
        
        while (!path.empty())
        {
            for (size_t i = 0; i < path.size(); ++i)
            {
                graph.add_flow(path[i], 1);
            }
            
            path = shortestPath(graph, from, to);
        }
        
        // parse nodes 1 - n and if there is a flow there is a match
        for (int i = 1; i <= n; ++i)
        {
            vector<size_t> edge_ids = graph.get_ids(i);
            
            for (size_t j = 0; j < edge_ids.size(); ++j)
            {
                // only use even ids
                if ( edge_ids[j] % 2 != 0)
                    continue;
                
                FlowGraph::Edge e = graph.get_edge(edge_ids[j]);
                
                if (e.flow == 1)
                {
                    matching[i - 1] = e.to - (n + 1);
                    break;
                }
            }
        }
        
        return matching;
    }
};

int main()
{
    std::ios_base::sync_with_stdio(false);
    MaxMatching max_matching;
    max_matching.Solve();
    return 0;
}