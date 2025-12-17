const recentGraphs = [];
const MAX_GRAPHS = 10;
const graphMap = new Map();

let lastGraphMsg = null;
const UPDATE_INTERVAL = 1000;
const DELETE_INTERVAL = 5000;

const socket = new WebSocket("ws://localhost:3001");

socket.onmessage = (msg) => {
    const outer = JSON.parse(msg.data);

    if (outer.event !== "graph") return;

    const data = JSON.parse(outer.data);
    graphMap.set(data.path, data);
    // lastGraphMsg = data;
};


function renderAllGraphs() {
    const container = d3.select("#chart");
    container.html("");

    recentGraphs.forEach((g, index) => {
        const div = container.append("div")
            .attr("class", "graph-wrapper")
            .style("margin-bottom", "40px");

        const date = new Date(g.time);
        // 제목
        div.append("h3")
            .html(`
        Graph #${recentGraphs.length - index}
        <br><span style="font-size: 12px; color: #555">
            path: ${g.path} <br>
            expected return: ${g.expected.toFixed(2)}% <br>
            time: ${date.toISOString()}
        </span>
            `)
            .style("margin", "10px 0 5px 0");

        renderGraph(div, g);

        g.nodes.forEach((n, index) => {
            div.append("h4").html(`${n.code}`);
        })
    });
}


function renderGraph(parentDiv, graph) {
    const width = 400;
    const height = 300;

    const svg = parentDiv
        .append("svg")
        .attr("width", width)
        .attr("height", height)
        .style("background", "#f8fafc")
        .style("border", "1px solid #ddd")
        .style("border-radius", "6px")
        .style("margin-bottom", "10px");

    const entryNode = graph.cycle[0];
    const exitNode = graph.cycle[graph.cycle.length - 1];

    const nodeNames = [...new Set(graph.cycle)];
    const nodes = nodeNames.map((id) => ({
        id, isEntry: id === entryNode, isExit: id === exitNode,
    }));

    const links = graph.edges.map((e) => ({
        source: e.from, target: e.to, weight: e.weight, type: e.type,
    }));

    const simulation = d3
        .forceSimulation(nodes)
        .force("link", d3.forceLink(links).id((d) => d.id).distance(120))
        .force("charge", d3.forceManyBody().strength(-300))
        .force("center", d3.forceCenter(width / 2, height / 2));

    const link = svg
        .selectAll("line")
        .data(links)
        .enter()
        .append("line")
        .attr("stroke", (d) => {
            if (d.type === "sell") return "#3b82f6"; // 파란색
            if (d.type === "buy") return "#ef4444"; // 빨간색
            return "#6366f1";
        })
        .attr("stroke-width", 2)


    const linkLabels = svg
        .selectAll(".link-label")
        .data(links)
        .enter()
        .append("text")
        .attr("font-size", "11px")
        .attr("fill", (d) => (d.type === "sell" ? "#3b82f6" : "#ef4444"))
        .text((d) => d.weight.toFixed(2));


    const node = svg
        .selectAll("circle")
        .data(nodes)
        .enter()
        .append("circle")
        .attr("r", 22)
        .attr("fill", (d) => {
            if (d.isEntry) return "#22c55e"; // entry = green
            if (d.isExit) return "#ef4444";  // exit = red
            return "#4f46e5";               // default
        })
        .attr("stroke", "#fff")
        .attr("stroke-width", 2);

    const nodeLabels = svg
        .selectAll(".node-label")
        .data(nodes)
        .enter()
        .append("text")
        .attr("text-anchor", "middle")
        .attr("dy", ".35em")
        .attr("fill", "#fff")
        .attr("font-size", "13px")
        .text((d) => d.id);

    simulation.on("tick", () => {
        link
            .attr("x1", (d) => d.source.x)
            .attr("y1", (d) => d.source.y)
            .attr("x2", (d) => d.target.x)
            .attr("y2", (d) => d.target.y);

        linkLabels
            .attr("x", (d) => (d.source.x + d.target.x) / 2)
            .attr("y", (d) => (d.source.y + d.target.y) / 2);

        node.attr("cx", (d) => d.x).attr("cy", (d) => d.y);
        nodeLabels.attr("x", (d) => d.x).attr("y", (d) => d.y);
    });
}

setInterval(() => {
    // if (!lastGraphMsg) {
    //     return;
    // }

    if (graphMap.size === 0) {
        return;
    }

    const container = d3.select("#chart");
    container.html("");
    const now = Date.now();
    for (const [key, value] of graphMap) {
        if (!value || now - value.time >= DELETE_INTERVAL) {
            graphMap.set(key, null);
            continue;
        }

        const div = container.append("div")
            .attr("class", "graph-wrapper")
            .style("margin-bottom", "40px");
        const date = new Date(value.time);

        div.append("h3")
            .html(`
        Graph #${key}
        <br><span style="font-size: 12px; color: #555">
            path: ${value.path} <br>
            expected return: ${value.expected.toFixed(2)}% <br>
            time: ${date.toISOString()}
        </span>
            `)
            .style("margin", "10px 0 5px 0");

        renderGraph(div, value);
    }


    // recentGraphs.push(lastGraphMsg);
    // if (recentGraphs.length > MAX_GRAPHS) {
    //     recentGraphs.shift();
    // }
    // renderAllGraphs();
    // lastGraphMsg = null;
}, UPDATE_INTERVAL);