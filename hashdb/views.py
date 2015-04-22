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
    resp = '{ "prooftree": [ { "pathNode": "9072e5c2ef9e2a21806fc8bb766b1a1da9dd18be0dd64d8e9da68dcc2e4574a4", "childNode": "null", "childDirection": "null" }, { "pathNode": "e482a5825985cc853e403cb580bd671c68ed311d27e736ae962d6a6edaf4e7f2", "childNode": "2f3caffd6aeec967a7d71eb7abec0993d036430691e668a8710248df4541111e", "childDirection": "right" }, { "pathNode": "32j1e5c2ef9e2a21806fc8bb766b1a1da9dd18be0dd64d8e9da68dcc2e4574a4", "childNode": "29d2d18be0dd64d8e9da68dcc2e4574a49072e5c2ef9e2a21806fc8bb766b1a1", "childDirection": "right" }, { "pathNode": "20a2e5c2ef9e2a21806fc8bb766b1a1da9dd18be0dd64d8e9da68dcc2e4574a4", "childNode": "baeed18be0dd64d8e9da68dcc2e4574a49072e5c2ef9e2a21806fc8bb766b1a1", "childDirection": "left" }, { "pathNode": "bbace5c2ef9e2a21806fc8bb766b1a1da9dd18be0dd64d8e9da68dcc2e4574a4", "childNode": "828dd18be0dd64d8e9da68dcc2e4574a49072e5c2ef9e2a21806fc8bb766b1a1", "childDirection": "right" }, { "pathNode": "9112e5c2ef9e2a21806fc8bb766b1a1da9dd18be0dd64d8e9da68dcc2e4574a4", "childNode": "bf9dd18be0dd64d8e9da68dcc2e4574a49072e5c2ef9e2a21806fc8bb766b1a1", "childDirection": "left" } ] }'
    return PlainResponse(resp)


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
