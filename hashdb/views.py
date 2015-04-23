from django.shortcuts import render
from django.http import HttpResponse
from django.db import connection
from django.core.urlresolvers import reverse
from django.views.decorators.csrf import csrf_exempt
import binascii

PlainResponse = lambda s: HttpResponse(s, content_type="text/plain")

# Create your views here.
def index(request):
    return PlainResponse("Hello world. This is HashCache. Served by Python Django.")

def proof_tree(request,proofHash):
    print(proofHash)
    response = '{ "prooftree": [ {'
    inputNodeId = None
    with connection.cursor() as c:
        c.execute("SELECT nodeID FROM NodeHash WHERE hex(hash)=%s", [proofHash])
        inputNodeId = c.fetchone()[0]
        response += '"pathNode": "' + proofHash + '" , "childNode": "null", "childDirection": "null" },{'
        print("inputi2D: " + str(inputNodeId))
    prevNode = inputNodeId
    finished = False
    while not finished:
        # Find ParentId
        print("hello")
        parentId = None
        with connection.cursor() as g:
            g.execute("SELECT parentID FROM Paternal WHERE nodeID=%s",[prevNode])
            parentId = g.fetchone()
            if parentId == None:
                print("none parent id")
                finished = True
                break
            else:
                parentId = parentId[0]
            childDirection = "right"
            g.execute("SELECT leftID FROM Siblings WHERE rightID=%s",[prevNode])
            siblingId = g.fetchone()
            if siblingId is None:
                g.execute("SELECT rightID FROM Siblings WHERE leftID=%s", [prevNode])
                siblingId = g.fetchone()
                if siblingId is None:
                    print("Everything's broken")
                else:
                    siblingId = siblingId[0]
                print("new sibling id" + str(siblingId))
                if(siblingId is None):
                    finished = True
                    break
            else:
                siblingId = siblingId[0]
                childDirection = "left"
            g.execute("SELECT hex(hash) FROM NodeHash WHERE nodeID=%s",[parentId])
            parentHash = g.fetchone()[0]
            g.execute("SELECT hex(hash) FROM NodeHash WHERE nodeID=%s",[prevNode])
            prevHash = g.fetchone()[0]
            g.execute("SELECT hex(hash) FROM NodeHash WHERE nodeID=%s",[siblingId])
            siblingHash = g.fetchone()[0]
        prevNode = parentId
        response += '"pathNode": "' + parentHash + '" , "childNode": "'+ siblingHash + '", "childDirection": "' + childDirection + '" },{'
    response = response[:-2] + "]}"
    print("response: " + response)
    return PlainResponse(response)


def recent_hashes(request):
    resp = ''
    with connection.cursor() as c:
        c.execute("SELECT hex(hash), uploadTime FROM SubmittedHashes order by uploadTime desc limit 10;")
        rows = c.fetchall()
        for row in rows:
            resp = resp+row[0] + "," + str(row[1]) + "\n"
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

def hash_info(request, hashhex):
    resp = 'error'
    with connection.cursor() as c:
        c.execute('SELECT min(nodeID) FROM NodeHash WHERE hash = unhex(%s) '
                'AND treeLevel=0', [hashhex])
        hid = c.fetchone()[0]
        if not hid:
            return PlainResponse('error: hash not submitted')
        c.execute('SELECT endTime IS NOT NULL from NodeHash, Window '
                'WHERE nodeId=%s and NodeHash.windowID=Window.windowID', [hid])
        row = c.fetchone()
        completedwindow = (row[0] == 1)

        c.execute('SELECT uploadTime FROM SubmittedHashes WHERE hash=unhex(%s)', [hashhex])
        uploadTime = c.fetchone()[0]

        resp =  'hash: %s\n' % hashhex
        resp += 'added: %s\n' % uploadTime

        if not completedwindow:
            resp +='\ncontained window not completed'
            return PlainResponse(resp)

        c.execute('call merklepath(%s)', [hashhex])
        rows = c.fetchall()
        resp += '\nmerkle path:\n' + '\n'.join([','.join(row) for row in rows])

        return PlainResponse(resp)
