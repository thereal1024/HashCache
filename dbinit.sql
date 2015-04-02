CREATE DATABASE HashCache;
USE HashCache;

CREATE TABLE Transaction(
	transactionID BINARY(32) PRIMARY KEY,
	hasFailed TINYINT(1) NOT NULL,
	confirmations INTEGER,
	includedBlock BINARY(32),
	fee BIGINT UNSIGNED, -- in satoshis
	address CHAR(36),
	txtime TIMESTAMP
);

CREATE TABLE MerkleRoot(
	nodeID INTEGER PRIMARY KEY
);

CREATE TABLE Proof(
	transactionID BINARY(32) PRIMARY KEY,
	windowID INTEGER NOT NULL
);

CREATE TABLE Paternal(
	nodeID INTEGER PRIMARY KEY,
	parentID INTEGER
);

CREATE TABLE Siblings(
	leftID INTEGER PRIMARY KEY,
	rightID INTEGER
);

CREATE TABLE NodeHash(
	nodeID INTEGER PRIMARY KEY AUTO_INCREMENT,
	hash BINARY(32) NOT NULL,
	windowID INTEGER NOT NULL,
	treeLevel INTEGER NOT NULL
);

CREATE TABLE SubmittedHashes(
	hash BINARY(32) PRIMARY KEY,
	uploadTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Window(
	windowID INTEGER PRIMARY KEY AUTO_INCREMENT,
	startTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	endTime TIMESTAMP NULL
);
