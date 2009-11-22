/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Peter Smith
 *******************************************************************************/

#ifndef PROTOCOL_H
#define PROTOCOL_H

#include "windows.h"
#include "XLCodec.h"

#ifdef PURE_CALL
class Protocol {
public:
	virtual ~Protocol() {}
	virtual int connect() = 0;
	virtual void disconnect() = 0;
	virtual bool isConnected() = 0;
	virtual void setHost(char* hostname) = 0;
	virtual void setPort(int port) = 0;
	virtual LPXLOPER execute(const char* name, LPXLOPER v0, LPXLOPER v1, LPXLOPER v2, LPXLOPER v3, 
		LPXLOPER v4, LPXLOPER v5, LPXLOPER v6, LPXLOPER v7, LPXLOPER v8, LPXLOPER v9) = 0 ;
	virtual LPXLOPER execute(const char* name) = 0;
};

class BinaryProtocol : public Protocol {
#else
class BinaryProtocol {
#endif
public:
	BinaryProtocol(char* hostname, int port) : port(port), conn(0), is(conn), os(conn) {
		setHost(hostname);
	}
	virtual ~BinaryProtocol() { disconnect(); }

	int connect();
	void disconnect();
	bool isConnected() { return conn != NULL; }
	void setHost(char* hostname) {
		strcpy(this->hostname, hostname);
	}
	void setPort(int port) {
		this->port = port;
	}
	LPXLOPER execute(const char* name, bool sendSource, int count, ...);
	LPXLOPER execute(const char* name, bool sendSource, int count, LPXLOPER far opers[]);

private:
	int send(const char* name, bool sendSource, int count, LPXLOPER far opers[]);
	LPXLOPER receive(const char* name);

private:
	char hostname[MAX_PATH];
	int port;
	SOCKET conn;
	XIStream is;
	XOStream os;
};

#endif // PROTOCOL_H