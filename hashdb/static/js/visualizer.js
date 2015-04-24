var network = null

function destroy() {
  if (network !== null) {
      network.destroy();
      network = null;
  }
}

function split_sha_string(str) {
  var result = '';
  while (str.length > 0) {
    result += str.substring(0, 16) + '\n';
    str = str.substring(16);
  }
  return result;
}

function draw(proofFile) {
  $("#postHash").show()
  destroy();
  var proof = JSON.parse(proofFile)
  proof=proof["prooftree"];
  nodeList = [];
  edges = [];
  var levels = proof.length-1;
  var prev = null;
  for(var i = 0; i<proof.length;i++){
    var nodes = proof[i];
    var nodeHash = nodes["pathNode"]
    var childHash = nodes["childNode"]
    nodeList.push({
      id: nodeHash,
      label: "PathNode" + String(i),
      level: levels-i
    })
    if(childHash!=null){
      nodeList.push({
        id: childHash,
        label: "SiblingNode "+ String(i-1),
        level: levels-i+1
      });
      if(nodes["childDirection"]=="right"){
        edges.push({
          from: nodeHash,
          to: prev
        });
        edges.push({
          from: nodeHash,
          to: childHash
        });  
      }
      else{
        edges.push({
          from: nodeHash,
          to: childHash
        });
        edges.push({
          from: nodeHash,
          to: prev
        });  
      }
      
    }
    prev = nodeHash;
  }
  buildNetwork(nodeList,edges);
}

function buildNetwork(nodeList,edges){
  console.log(nodeList);
  console.log(edges);
  nodeList[0]["label"] = "Leaf Node"
  nodeList[nodeList.length-2]["label"] = "Merkle Root"
  // create a network
  var container = document.getElementById('graphImage');
  var data = {
    nodes: nodeList,
    edges: edges
  };

  var options = {
    hierarchicalLayout: {
        direction: "UD"
    },
    nodes: {
        radiusMax:10,
        fontSize:16
    },
    width: '400px',
    height: '550px'
  };
  console.log(container)
  var network = new vis.Network(container, data, options);
  $("#hashForm").hide()
  // add event listeners
  network.on('select', function(params) {
    document.getElementById('selection').innerHTML = '<b>Hash Value: </b>' + params.nodes;
  });

}

function hashValues(){
  var leftHash = document.getElementById("lefthash").value 
  var rightHash = document.getElementById("righthash").value
  var binLeft = hex2bin(leftHash)
  var binRight = hex2bin(rightHash)
  console.log("test")
  console.log(binLeft)
  var concatHash = CryptoJS.SHA256(CryptoJS.SHA256(CryptoJS.enc.Hex.parse(leftHash + rightHash))).toString(CryptoJS.enc.Hex)
  document.getElementById('answer').innerHTML = "<div class='form-group'><b>Concatenated Hash :</b> " + concatHash + "<br /></div>"
  console.log(concatHash)
}
