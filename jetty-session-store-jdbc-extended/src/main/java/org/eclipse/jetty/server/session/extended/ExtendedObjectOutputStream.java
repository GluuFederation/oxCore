package org.eclipse.jetty.server.session.extended;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

public class ExtendedObjectOutputStream extends java.io.ObjectOutputStream {

	public ExtendedObjectOutputStream(OutputStream out) throws IOException {
		super(out);
		enableReplaceObject(true);
	}

	@Override
	protected Object replaceObject(Object obj) throws IOException {
		if (obj instanceof Serializable) {
			return obj;
		}

		return null;
	}

	public class NullOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {}
	}

}
