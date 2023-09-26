package org.eclipse.jetty.server.session.extended;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtendedObjectOutputStream extends java.io.ObjectOutputStream {

	private static final Logger LOG = LoggerFactory.getLogger(ExtendedObjectOutputStream.class);

	private NullOutputStream nos = new NullOutputStream();
	private ObjectOutputStream oos = new ObjectOutputStream(nos);

	private boolean _serializationLogSkipped;

	public ExtendedObjectOutputStream(OutputStream out, boolean serializationLogSkipped) throws IOException {
		super(out);
		enableReplaceObject(true);
		this._serializationLogSkipped = serializationLogSkipped;
	}

	@Override
	protected Object replaceObject(Object obj) throws IOException {
		try {
			oos.writeObject(obj);

			return obj;
		} catch (Throwable ex) {
			if (obj instanceof Serializable) {
				return obj;
			}

			if (_serializationLogSkipped) {
				LOG.warn("Skipping object <{}> serialization, class <{}>  ", obj, obj.getClass());
			}
		}

		return null;
	}

	public class NullOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {}
	}

}
