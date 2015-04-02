drop database cashcache;
source dbinit.sql;
source hashins.sql;
-- case 3 nodes
call addhash(sha2('wat',256), @ok);
call addhash(sha2('lol',256), @ok);
call addhash(sha2('aaa',256), @ok);
call windowopen(@nwin);
select nodeID, hex(hash), windowID, treeLevel from NodeHash;
select * from Siblings;
select * from Paternal;
call windowroot(@nwin-1, @wr);
select @wr;
-- case 5 nodes
call addhash(sha2('wat',256), @ok);
select @ok;
call addhash(sha2('wat1',256), @ok);
select @ok;
call addhash(sha2('lol1',256), @ok);
call addhash(sha2('aaa1',256), @ok);
call addhash(sha2('but',256), @ok);
call addhash(sha2('asdfasdf',256), @ok);
call windowopen(@nwin);
select nodeID, hex(hash), windowID, treeLevel from NodeHash;
select * from Siblings;
select * from Paternal;
call windowroot(@nwin-1, @wr);
select @wr;
-- case 2 nodes
call addhash(sha2('ah',256), @ok);
call addhash(sha2('ha',256), @ok);
call windowopen(@nwin);
select nodeID, hex(hash), windowID, treeLevel from NodeHash;
select * from Siblings;
select * from Paternal;
call windowroot(@nwin-1, @wr);
select @wr;
