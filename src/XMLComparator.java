import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.CREATE;

public class XMLComparator {
	public static void main(String[] args) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
		if (args.length < 1 || !Files.exists(Paths.get(args[0]))) {
			System.out.println(String.format("Invalid file name: %s", args.length < 1 ? "null" : args[0]));
			return;
		}

		Path input = Paths.get(args[0]);
		try (InputStream reader = Files.newInputStream(Paths.get(args[0]))) {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.parse(reader);
			document.getDocumentElement().normalize();
			Element root = document.getDocumentElement();
			NodeList nList = null;
			if (args.length > 1 && args[1] != null && !args[1].isEmpty()) {
				NodeList tmpList = document.getElementsByTagName(args[1]);
				if (tmpList != null) nList = tmpList;
			}
			if (nList == null) nList = root.getChildNodes();

			System.out.println(String.format(
				"Converting xml file %s to an output plain file %s", input.getFileName().toString(),String.format("%s.%s", getFileName(input.getFileName()), "out"))
			);

			try (BufferedWriter output = Files.newBufferedWriter((Paths.get(String.format("%s.%s", getFileName(input.getFileName()), "out"))), Charset.forName("UTF-8"), CREATE)) {
				walkNode(output, nList, null);
				output.flush();
			}
			System.out.println(String.format(
				"Finished!")
			);
		}
	}

	private static String getFileName(Path fileNamePath) {
		String fileName = fileNamePath.getFileName().toString();
		if (fileName.indexOf(".") > 0) fileName = fileName.substring(0, fileName.lastIndexOf("."));
		return fileName;
	}

	private static void walkNode(BufferedWriter op, NodeList nList, String prefix) throws IOException {
		for (int i = 0; i < nList.getLength(); i++)
		{
			Node node = nList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				String[] nodeNameArr = node.getNodeName().split(":");
				String nodeName = prefix == null ? nodeNameArr[1] : String.format("%s.%s", prefix, nodeNameArr[1]);
				if (node.getChildNodes().getLength() == 1) {
					op.write(nodeName);
					op.newLine();
				}
				if (node.hasChildNodes()) {
					walkNode(op, node.getChildNodes(), nodeName);
				}
			}
		}
	}

}
