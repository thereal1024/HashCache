from django.shortcuts import render
from django.http import HttpResponse, Http404
from django.db import connection
from django.core.urlresolvers import reverse
from django.views.decorators.csrf import csrf_exempt
import binascii
import json

PlainResponse = lambda s: HttpResponse(s, content_type="text/plain")

# Create your views here.
def index(request):
    return PlainResponse("Hello world. This is HashCache. Served by Python Django.")

def recent_hashes(request):
    resp = ''
    with connection.cursor() as c:
        c.execute("SELECT hex(hash), uploadTime FROM SubmittedHashes order by uploadTime desc limit 10;")
        rows = c.fetchall()
        for row in rows:
            resp = resp+row[0].lower() + "," + str(row[1]) + "\n"
    #Remove railing new line
    resp = resp[:-1]
    return PlainResponse(resp)


def open_window(request):
    resp = 'error'
    with connection.cursor() as c:
        c.execute('call windowopen(@newwin)')
        c.execute('select @newwin')
        row = c.fetchone()
        resp = row[0]
    return PlainResponse(resp)

def view_window(request, winnum):
    resp = 'error'
    with connection.cursor() as c:
        winnum = int(winnum)
        c.execute('select hex(hash) from NodeHash where '
                'treeLevel=0 and windowID=%s', [winnum])
        rows = c.fetchall()
        resp = '\n'.join([row[0]for row in rows])
    return PlainResponse(resp)

@csrf_exempt
def submit_hash(request):
    if request.method != 'POST':
        return PlainResponse("error: use post")
    hv = request.POST.get('hash')
    try:
        bd = binascii.unhexlify(hv)
        if len(bd) != 32:
            raise Exception
    except Exception:
        return PlainResponse("error: not valid sha256 hash")

    resp = 'error'
    with connection.cursor() as c:
        c.execute('call addhash(%s, @ok)', [hv])
        c.execute('select @ok')
        row = c.fetchone()
        ok = (row[0] == 1)
        if ok:
            resp = 'OK'
        else:
            resp = 'error: hash already submitted'
    return PlainResponse(resp)

def lookup_path(hashhex):
    submitted, complete = False, False
    with connection.cursor() as c:
        c.execute('SELECT min(nodeID) FROM NodeHash WHERE hash = unhex(%s) '
                'AND treeLevel=0', [hashhex])
        hid = c.fetchone()[0]
        if not hid:
            return False, False, None, [], None, None

        submitted = True
        
        c.execute('SELECT endTime IS NOT NULL, NodeHash.windowID from NodeHash, Window '
                'WHERE nodeId=%s and NodeHash.windowID=Window.windowID', [hid])
        row = c.fetchone()
        completedwindow = (row[0] == 1)
        window = row[1]

        c.execute('SELECT uploadTime FROM SubmittedHashes WHERE hash=unhex(%s)', [hashhex])
        uploadTime = c.fetchone()[0]

        path = None
        merkleroot = None
        if completedwindow:
            c.execute('call merklepath(%s)', [hashhex])
            path = c.fetchall()
            
            c.execute('SELECT hex(NodeHash.hash) FROM MerkleRoot, NodeHash '
                'WHERE NodeHash.nodeID = MerkleRoot.nodeID '
                'AND NodeHash.windowID = %s', [window])
            merkleroot = c.fetchone()[0]
            
            complete = True
    return submitted, complete, uploadTime, path, window, merkleroot

def lookup_tx(window):
    with connection.cursor() as c:
        c.execute('SELECT Transaction.transactionID, rawdata, blockpath, txtime, '
            'confirmations, includedBlock '
            'FROM Transaction, Proof '
            'WHERE Transaction.transactionID = Proof.transactionID '
            'AND Proof.windowID = %s', [window])
        row = c.fetchone()
        if not row:
            return None, None, None, None
        
        tx_exists = True
        txid, rawtx, packed_path, txtime, confirms, blockid = row
        if txid:
            txid = binascii.hexlify(txid).decode()
        if rawtx:
            rawtx = binascii.hexlify(rawtx).decode()
        if blockid:
            blockid = binascii.hexlify(blockid).decode()
        proofready = (confirms > 0)
        
        # process packed path
        splitn = lambda line,n: [line[i:i+n] for i in range(0, len(line), n)]
        if packed_path:
            pathelem = splitn(binascii.hexlify(packed_path).decode(),  33*2)
            pathelem = [(e[:2],e[2:]) for e in pathelem]
            blockpath = []
            for side_rep, hashcode in pathelem:
                blockpath.append((hashcode.lower(), 'right' if side_rep == '00' else 'left'))
    
    return tx_exists, proofready, txid, txtime, rawtx, blockid, blockpath
    
def hash_proof(request, hashhex):
    hashhex = hashhex.lower()
    submitted, complete, uploadTime, path, window, merkleroot = lookup_path(hashhex)
    
    if not submitted:
        raise Http404('error: hash not submitted')
    
    if not complete: 
        raise Http404('error: contained window not completed')
    
    tx_exists, proofready, txid, txtime, rawtx, blockid, blockpath = lookup_tx(window)
    if not proofready:
        raise Http404('error: proof not ready')
    
    jproof = dict()
    jproof['fileHash'] = hashhex
    jproof['txMerklePath'] = [(row[1].lower(),row[2]) for row in path]
    jproof['txData'] = rawtx
    jproof['blockMerklePath'] = blockpath
    jproof['blockID'] = blockid
    
    return PlainResponse(json.dumps(jproof))

def hash_info(request, hashhex):
    hashhex = hashhex.lower()
    submitted, complete, uploadTime, path, window, merkleroot = lookup_path(hashhex)
    
    if not submitted:
        return PlainResponse('error: hash not submitted')
    
    resp =  'hash: %s\n' % hashhex
    resp += 'added: %s\n' % uploadTime
    resp += 'window: %s\n' % window
    
    if not complete: 
        resp +='\ncontained window not completed'
        return PlainResponse(resp)
        
    resp += '\nmerkle path:\n' + '\n'.join([(row[1].lower() + ',' + row[2]) for row in path])
    return PlainResponse(resp)
    
def proof_tree(request,hashhex):
    hashhex = hashhex.lower()
    submitted, complete, uploadTime, path, window, merkleroot = lookup_path(hashhex)
    
    if submitted and complete: 
        if len(path) > 0:      
            nodes, siblings, sides = zip(*path)
        else:
            nodes, siblings, sides = (),(),()
        nodes = tuple(map(str.lower, nodes)) + (merkleroot.lower(),)
        children = (None,) + tuple(map(str.lower, siblings))
        sides = (None,) + sides
        
        entries = []
        for node, child, side in zip(nodes, children, sides):
            entries.append({
                'pathNode': node, 
                'childNode': child, 
                'childDirection': side,
            })
        
        resp = json.dumps({'prooftree': entries})
        return PlainResponse(resp)
        
    #resp = '{ "prooftree": [ { "pathNode": "9072e5c2ef9e2a21806fc8bb766b1a1da9dd18be0dd64d8e9da68dcc2e4574a4", "childNode": "null", "childDirection": "null" }, { "pathNode": "e482a5825985cc853e403cb580bd671c68ed311d27e736ae962d6a6edaf4e7f2", "childNode": "2f3caffd6aeec967a7d71eb7abec0993d036430691e668a8710248df4541111e", "childDirection": "right" }, { "pathNode": "32j1e5c2ef9e2a21806fc8bb766b1a1da9dd18be0dd64d8e9da68dcc2e4574a4", "childNode": "29d2d18be0dd64d8e9da68dcc2e4574a49072e5c2ef9e2a21806fc8bb766b1a1", "childDirection": "right" }, { "pathNode": "20a2e5c2ef9e2a21806fc8bb766b1a1da9dd18be0dd64d8e9da68dcc2e4574a4", "childNode": "baeed18be0dd64d8e9da68dcc2e4574a49072e5c2ef9e2a21806fc8bb766b1a1", "childDirection": "left" }, { "pathNode": "bbace5c2ef9e2a21806fc8bb766b1a1da9dd18be0dd64d8e9da68dcc2e4574a4", "childNode": "828dd18be0dd64d8e9da68dcc2e4574a49072e5c2ef9e2a21806fc8bb766b1a1", "childDirection": "right" }, { "pathNode": "9112e5c2ef9e2a21806fc8bb766b1a1da9dd18be0dd64d8e9da68dcc2e4574a4", "childNode": "bf9dd18be0dd64d8e9da68dcc2e4574a49072e5c2ef9e2a21806fc8bb766b1a1", "childDirection": "left" } ] }'
    raise Http404('hash does not exist')
    #return PlainResponse(resp)
