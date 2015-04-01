
DELIMITER //
CREATE PROCEDURE addhash (IN hashhex CHAR(64)) 
BEGIN
	SET @HASH = unhex(hashhex);
	-- insert into submitted hashes
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
CREATE PROCEDURE windowopen (OUT newwin INTEGER) 
BEGIN
	SET @WIN = (select max(windowID) from Window);
	IF @WIN IS NOT NULL THEN
		UPDATE Window SET endTime = NOW() WHERE windowID = @WIN;
	END IF;
	INSERT INTO Window VALUES (NULL, NULL, NULL);
	SET newwin = LAST_INSERT_ID();
END //

DELIMITER ;

-- open first window
CALL windowopen(@LWIN);
