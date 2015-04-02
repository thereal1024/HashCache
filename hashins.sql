
DELIMITER //
CREATE PROCEDURE addhash (IN hashhex CHAR(64), OUT ok INT) 
addhashmain:BEGIN
	SET @HASH = unhex(hashhex);
	-- insert into submitted hashes, or fail if already there
	SET ok = (@HASH) not in (SELECT hash FROM SubmittedHashes);
	IF NOT ok THEN
		LEAVE addhashmain;
	END IF;
	INSERT INTO SubmittedHashes VALUES (@HASH, NULL);
	
	SET @WIN = (select max(windowID) from Window);

	-- insert new node for hash
	INSERT INTO NodeHash VALUES (NULL, @HASH, @WIN, 0);
	SET @CURID = LAST_INSERT_ID();

	SET @INDEX = 0;

	-- tree join loop
	joinloop: LOOP
		-- attempt to find sibling
		SET @SIBL = (SELECT Siblings.leftID FROM NodeHash, Siblings
			WHERE Siblings.leftID = NodeHash.nodeID
			AND NodeHash.treeLevel = @INDEX
			AND NodeHash.windowID = @WIN
			AND Siblings.rightID IS NULL);
			
		IF @SIBL IS NOT NULL THEN
			-- new node is sibling on the right
			UPDATE Siblings SET rightID = @CURID WHERE leftID = @SIBL;
			-- hash for parent
			SET @HASH = unhex(sha2(sha2(CONCAT(
			(SELECT hash FROM NodeHash WHERE NodeId = @SIBL),
			@HASH), 256), 256));
			-- create parent and capture id
			INSERT INTO NodeHash VALUES (NULL, @HASH, @WIN, @INDEX + 1);
			SET @NEWNODE = LAST_INSERT_ID();
			-- point children to parent
			UPDATE Paternal SET parentID = @NEWNODE WHERE nodeID = @SIBL;
			INSERT INTO Paternal VALUES (@CURID, @NEWNODE);
			-- current id moves up
			SET @CURID = @NEWNODE;
			
		ELSE
			-- no sibling -> done
			INSERT INTO Siblings VALUES (@CURID, NULL);
			INSERT INTO Paternal VALUES (@CURID, NULL);
			LEAVE joinloop;
		END IF;
		
		SET @INDEX = @INDEX + 1;
	END LOOP joinloop;
END //

DELIMITER ;

DELIMITER //
CREATE PROCEDURE treecomplete (IN win INTEGER) 
BEGIN
	SET @MAXL = (SELECT COALESCE(max(treeLevel),-1) FROM NodeHash WHERE windowID = win);
	SET @INDEX = 0;
	SET @WIN = win;
	SET @SEEK = TRUE;
	
	WHILE @INDEX <= @MAXL DO
		-- find unpaired sibling at level=index
		SET @UNPAIR = (SELECT Siblings.leftID FROM NodeHash, Siblings
			WHERE Siblings.leftID = NodeHash.nodeID
			AND NodeHash.treeLevel = @INDEX
			AND NodeHash.windowID = @WIN
			AND Siblings.rightID IS NULL);
		
		IF @SEEK THEN
			IF @UNPAIR IS NOT NULL THEN
				SET @HASH = (SELECT hash FROM NodeHash WHERE NodeId = @UNPAIR);
				SET @CURID = @UNPAIR;
				IF @INDEX < @MAXL THEN
					SET @SEEK = FALSE;
				END IF;
			END IF;
		END IF;
			
		IF NOT @SEEK THEN
			IF @UNPAIR IS NOT NULL THEN
				-- joining with a unpaired node
			
				-- link unpaired as sibling to raised node
				UPDATE Siblings SET rightID = @CURID WHERE leftID = @UNPAIR;
				-- join hash values
				SET @HASH = unhex(sha2(sha2(CONCAT(
					(SELECT hash FROM NodeHash WHERE NodeId = @UNPAIR),
					@HASH), 256), 256));
				-- create new parent and capture id
				INSERT INTO NodeHash VALUES (NULL, @HASH, @WIN, @INDEX + 1);
				SET @NEWNODE = LAST_INSERT_ID();
				-- point 2 children up
				UPDATE Paternal SET parentID = @NEWNODE WHERE
					nodeID = @UNPAIR OR nodeID = @CURID;
				-- init paternal for new node
				INSERT INTO Paternal VALUES (@NEWNODE, NULL);
				-- move up
				SET @CURID = @NEWNODE;
			ELSE
			    -- join node with itself
			    
				-- last created node should be sibling with itself
				INSERT INTO Siblings VALUES (@CURID, @CURID);
				-- join hash with itself
				SET @HASH = unhex(sha2(sha2(CONCAT(@HASH,@HASH), 256), 256));
				-- create new parent and capture id
				INSERT INTO NodeHash VALUES (NULL, @HASH, @WIN, @INDEX + 1);
				SET @NEWNODE = LAST_INSERT_ID();
				-- point 1 child up
				UPDATE Paternal SET parentID = @NEWNODE WHERE nodeID = @CURID;
				-- init paternal for new node
				INSERT INTO Paternal VALUES (@NEWNODE, NULL);
				-- move up
				SET @CURID = @NEWNODE;
			END IF;
		END IF;
		
		SET @INDEX = @INDEX + 1;
	END WHILE;
	
	-- if we merged at all, the last created node needs a no sibling assignment
	-- normally the sibling is handled the first part of the next iteration
	-- but that means the last iteration's new node didn't get a sibling insert
	IF NOT @SEEK THEN
		INSERT INTO Siblings VALUES (@CURID, NULL);
	END IF;
	
	-- TODO list merkle root etc.
	INSERT INTO MerkleRoot VALUES (@CURID);
	
END //

DELIMITER ;


DELIMITER //
CREATE PROCEDURE windowopen (OUT newwin INTEGER) 
BEGIN
	SET @WIN = (select max(windowID) from Window);
	
	IF @WIN IS NOT NULL THEN
		UPDATE Window SET endTime = NOW() WHERE windowID = @WIN;
	END IF;
	INSERT INTO Window VALUES (NULL, NULL, NULL);
	SET newwin = LAST_INSERT_ID();
	IF @WIN IS NOT NULL THEN
		CALL treecomplete(@WIN);
	END IF;

END //

DELIMITER ;

DELIMITER //
CREATE PROCEDURE merklepath (IN hashhex CHAR(64)) 
BEGIN
	-- will return emptyset if hash not in COMPLETED tree
	SET @TARGET = (SELECT min(nodeID) FROM NodeHash WHERE hash = unhex(hashhex));

	select hex(hash) as 'sibling', V.side from NodeHash,
	(select 'left' as side union select 'right' as side) as V
	where
	(nodeID,V.side) in (
		select IF(fwd.other=nodeID,bck.other,fwd.other) as 'comp',
		IF(fwd.other=nodeID,'left','right') as 'side' from NodeHash, 
		(select leftID as 'match', rightID as 'other' from Siblings) as fwd,
		(select rightID as 'match', leftID as 'other' from Siblings) as bck
		where 
		(fwd.match = bck.other)
		and ((nodeID = fwd.match) or (nodeID = bck.match))
		and nodeID in
			(select nodeID from (select nodeID, @pv:=parentID as 'parentID' from Paternal
			join
			(select @pv:=@TARGET)tmp
			where nodeID=@pv) as nodeparents)
		order by comp -- this ordering might not be necessary
	);
END //


DELIMITER //
CREATE PROCEDURE windowroot (IN winnum INTEGER, OUT root BINARY(32)) 
BEGIN
	SET root = (SELECT MerkleRoot.nodeID FROM MerkleRoot, NodeHash 
		WHERE NodeHash.nodeID = MerkleRoot.nodeID
		AND NodeHash.windowID = winnum);
END //

DELIMITER ;

-- open first window
CALL windowopen(@LWIN);

