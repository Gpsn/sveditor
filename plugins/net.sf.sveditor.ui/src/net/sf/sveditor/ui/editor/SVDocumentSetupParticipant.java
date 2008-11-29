package net.sf.sveditor.ui.editor;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;

public class SVDocumentSetupParticipant implements IDocumentSetupParticipant {

	public void setup(IDocument doc) {
		if (doc instanceof IDocumentExtension3) {
			IDocumentExtension3 docExt = (IDocumentExtension3)doc;
			IDocumentPartitioner part = new FastPartitioner(
					new SVPartitionScanner(),
					SVDocumentPartitions.SV_PARTITION_TYPES);
			docExt.setDocumentPartitioner(SVDocumentPartitions.SV_PARTITIONING, part);
			part.connect(doc);
		}
	}
}