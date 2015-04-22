"""
Visualizer that takes in a prooftree json format
and produces a png file visual representation
of the associated proof tree.

Usage: python visualizer.py data.json

Key:

Green Nodes : Path nodes in the tree
Yellow Nodes : Retrieved sibling nodes to create the tree
Brown Node : Leaf node that the proof is called for
Node Number : What order the children need to be arranged in
            in order to hash to create the parent
            ie: 1 = left child , 2 = right child, 3 = parent node
            3 = hash (1 + 2)
"""
import pydot
import json
import re
import sys

def import_json(fileName):
    with open(fileName) as proof_tree:
        proof = json.load(proof_tree)
    return proof

def split_sha_string(sha):
    return re.sub("(.{16})","\\1\n",sha,0,re.DOTALL)

def print_help(string):
    print(string)
#Import json
if(len(sys.argv)<1):
    print_help("Error, no file given")
    sys.exit(0)
data = import_json(sys.argv[1])
tree_array = data['prooftree']

#Graph initialization
graph = pydot.Dot(graph_type="digraph")
prev = None
a = 0
for nodes in tree_array:
    splitLabel = split_sha_string(nodes['pathNode'])
    #Assign label numbers
    fillColor = "green"
    if(nodes['childNode']=="null"):
        fillColor="#976856"
        if(tree_array[a+1]["childDirection"]=="right"):
            splitLabel = splitLabel + "\n1"
        else:
            splitLabel = splitLabel + "\n2"
    elif(a+1<len(tree_array)):
        father = tree_array[a+1]
        if(father["childDirection"]=="right"):
            splitLabel = splitLabel + "\n1"
        else:
            splitLabel = splitLabel + "\n2"
    #Add the path node
    node = pydot.Node("pathNode"+ str(a), label=splitLabel, style="filled", fillcolor=fillColor)
    graph.add_node(node)
    #Add child label and node
    if(nodes['childNode']!="null"):
        splitLabel = split_sha_string(nodes['childNode'])
        if(nodes['childDirection']=="left"):
            splitLabel = splitLabel + "\n1"
        else:
            splitLabel = splitLabel + "\n2"
        child_node = pydot.Node("child"+str(a), label=splitLabel, style="filled", fillcolor="yellow")
        graph.add_node(child_node)
        graph.add_edge(pydot.Edge(node,child_node))
    if(prev is not None):
        graph.add_edge(pydot.Edge(node,prev))
    prev = node
    a +=1

graph.write_png('proof_visualization.png')
