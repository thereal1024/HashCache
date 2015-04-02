from django.shortcuts import render
from django.http import HttpResponse
from django.db import connection
from django.views.decorators.csrf import csrf_exempt
import binascii

# Create your views here.
def index(request):
    return HttpResponse("Hello world. This is HashCache. Served by Python Django.")

def open_window(request):
	resp = 'error'
	with connection.cursor() as c:
		c.execute('call windowopen(@newwin)')
		c.execute('select @newwin')
		row = c.fetchone()
		resp = row[0]
	return HttpResponse(resp)

def view_window(request):
	resp = 'error'
	with connection.cursor() as c:
		c.execute('select hex(hash) from NodeHash where '
			'treeLevel=0 and windowID=1')
		rows = c.fetchall()
		resp = '\n'.join([row[0]for row in rows])
	return HttpResponse(resp)

@csrf_exempt
def submit_hash(request):
	if request.method != 'POST':
		return HttpResponse("error: use post")
	hv = request.POST.get('hash')
	try:
		bd = binascii.unhexlify(hv)
		if len(bd) != 32:
			raise Exception
	except Exception:
		return HttpResponse("error: not valid sha256 hash")
	
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
	return HttpResponse(resp)
