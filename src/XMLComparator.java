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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.CREATE;

public class XMLComparator {
	public static void main(String[] args) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
		if (args.length < 1 || !Files.exists(Paths.get(args[0]))) {
			System.out.println(String.format("Invalid file name: %s", args.length < 1 ? "null" : args[0]));
			return;
		}
		String fileNameToCompare = null;

		Path input = Paths.get(args[0]);
		boolean compare = false;
		try (InputStream reader = Files.newInputStream(Paths.get(args[0]))) {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.parse(reader);
			document.getDocumentElement().normalize();
			Element root = document.getDocumentElement();
			NodeList nList = null;

			if (args.length > 1) {
				if (args[1] != null && !args[1].isEmpty() && args[1].contains(":")) {
					NodeList tmpList = document.getElementsByTagName(args[1]);
					if (tmpList != null) nList = tmpList;
				}

				compare = !args[1].contains(":") && Files.exists(Paths.get(args[1]));
				if (compare) fileNameToCompare  = args[1];

				if (!compare && args.length > 2) {
					compare = Files.exists(Paths.get(args[2]));
					if (compare) fileNameToCompare  = args[2];
				}
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

			if (compare) {
				System.out.println(String.format(
						"Wait, you want to compare something!")
				);

				System.out.println(String.format(
						"Comparing %s vs %s", String.format("%s.%s", getFileName(input.getFileName()), "out"), fileNameToCompare)
				);

				compare(String.format("%s.%s", getFileName(input.getFileName()), "out"), fileNameToCompare);
			}
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

	private static void compare(String fp1, String fp2) throws IOException {
		Path p1 = Paths.get(fp1);
		Path p2 = Paths.get(fp2);
		List<String> words1 = new ArrayList<>();
		List<String> words2 = new ArrayList<>();


		try (Stream<String> stream = Files.lines(p2)) {
			final List<String> tmpWords1 = stream
					.filter(line -> !line.isEmpty())
					.filter(line -> line != null)
					.sorted((s1, s2) -> s2.compareTo(s1))
					.collect(Collectors.toList());

			Predicate<String> match = new Predicate<String>() {
				@Override
				public boolean test(String s) {
					String r = tmpWords1.stream().filter(l -> l.matches(String.format("%s\\..*", s))).findFirst().orElse("Nothing");
					return r.equals("Nothing");
				}
			};

			words1 = tmpWords1.stream()
					.filter(match)
					.sorted((s1, s2) -> s1.compareTo(s2))
					.collect(Collectors.toList());

		} catch (IOException e) {
			e.printStackTrace();
		}

		try (Stream<String> stream = Files.lines(p1)) {
			words2 = stream
					.filter(line -> !line.isEmpty())
					.filter(line -> line != null)
					.sorted((s1, s2) -> s1.compareTo(s2))
					.collect(Collectors.toList());

		} catch (IOException e) {
			e.printStackTrace();
		}

		try (BufferedWriter output = Files.newBufferedWriter((Paths.get(String.format("%s.%s", fp1, "txt"))), Charset.forName("UTF-8"), CREATE)) {
			int c = 0;
			output.write(String.format("The following elements were not found in %s", fp2));
			output.newLine();
			for (String w : words1) {
				if (!words2.contains(w)) {
					c++;
					output.newLine();
					output.write("\t");
					output.write(w);
				}
			}

			output.newLine();
			output.newLine();
			output.write(String.format("Total elements not found %d of %d", c, words1.size()));
			output.newLine();
			output.newLine();
			output.write(String.format("The following elements were not found in %s", fp1));
			output.newLine();
			c = 0;
			for (String w : words2) {
				if (!words1.contains(w)) {
					c++;
					output.newLine();
					output.write("\t");
					output.write(w);
				}
			}

			output.newLine();
			output.newLine();
			output.write(String.format("Total elements not found %d of %d", c, words2.size()));
			output.flush();
		}
	}


}
