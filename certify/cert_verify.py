import hashlib
import binascii
import json
import sys
import time
import urllib.request
from functools import partial

APILOC = 'https://insight.bitpay.com/api/block/%s'
APIJSONMERKLE = 'merkleroot'
APIJSONTIME = 'time'

if len(sys.argv) < 3:
    print('Usage: python3 %s [file_to_check] [proof]' % sys.argv[0])
    #print('Proof file only must be supplied when it is NOT [file_to_check].dproof')
    sys.exit(1)

# double sha256
h2=lambda x: hashlib.sha256(hashlib.sha256(x).digest()).digest()
# unhex
u=lambda x: binascii.unhexlify(x)
# hex
x=lambda x: binascii.hexlify(x)
# reverse (for binary only)
r=lambda x: x[::-1]

CHUNK_SIZE = 1024 * 1024
filedigester = hashlib.sha256()
with open(sys.argv[1], 'rb') as file_to_check:
    for chunk in iter(partial(file_to_check.read, CHUNK_SIZE), b''):
        filedigester.update(chunk)

filehash = filedigester.digest() # binary digest

def pathwalk(hashval, path):
    for comphex, compside in path:
        compbin = u(comphex)
        if compside == 'left':
            join = compbin + hashval
        elif compside == 'right':
            join = hashval + compbin
        else:
            raise Exception('invalid hash side value: %s' % compside)
        hashval = h2(join)
        #print(x(hashval))
    return hashval

with open(sys.argv[2], 'r') as proof_file:
    proof = json.load(proof_file)
    check_file_hash = proof['fileHash']
    if filehash != u(check_file_hash):
        print('FAIL: file hash does not match up with hash assocated with proof')
        print('input file: %s\nproof is for hash: %s' % (x(filehash).decode(),check_file_hash))
        sys.exit(1)
    txpath = proof['txMerklePath']
    whash = pathwalk(filehash, txpath)
    txdata = proof['txData']
    if whash not in u(txdata):
        print('FAIL: window merkle root %s not in provided transaction data:\n%s' % (x(whash).decode(), txdata))
        sys.exit(1)
    whash = h2(u(txdata))
    block_path = proof['blockMerklePath']
    merkle_root_be = pathwalk(whash, block_path)
    merkle_root_le = r(merkle_root_be)
    try:
        rq = urllib.request.urlopen(APILOC % proof['blockID'])
        resp = json.loads(rq.read().decode())
    except Exception:
        print('Error: Unable to connect to API to lookup info on block')
        sys.exit(1)

    if resp[APIJSONMERKLE] != x(merkle_root_le).decode():
        print('FAIL: Associated proof transaction failed to be demonstrated in the '
            + 'mentioned block ID: %s' % proof['blockID'])
        sys.exit(1)
    sec_since_epoch = resp[APIJSONTIME]
    print('File proved to exist on %s' % (time.ctime(sec_since_epoch)))
    
