package de.typology.utils;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.PriorityQueue;

public class MyCollector extends Collector {
	public class CustomQueue extends PriorityQueue<Document> {

		public CustomQueue(int maxSize) {
			super(maxSize);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected boolean lessThan(Document arg0, Document arg1) {
			String tmp = arg0.get("cnt");
			if (tmp == null) {
				return false;
			}
			Float f0 = Float.parseFloat(tmp);
			tmp = arg1.get("cnt");
			if (tmp == null) {
				return true;
			}
			Float f1 = Float.parseFloat(tmp);
			return f0.compareTo(f1) < 0;
		}
	}

	public CustomQueue _queue = null;
	IndexReader _currentReader;

	public MyCollector(int maxSize) {
		this._queue = new CustomQueue(maxSize);
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return true;
	}

	@Override
	public void collect(int arg0) throws IOException {
		this._queue.insertWithOverflow(this._currentReader.document(arg0));
	}

	@Override
	public void setNextReader(AtomicReaderContext arg0) throws IOException {
		this._currentReader = arg0.reader();

	}

	@Override
	public void setScorer(Scorer arg0) throws IOException {
		// TODO Auto-generated method stub

	}
}