package bfbc.jiggity.compiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaCompilerTool {

	private static Logger logger = LoggerFactory.getLogger(JavaCompilerTool.class);

	public static class SourceInMemory {
		public final String filePath;
		public final String sourceCode;

		public SourceInMemory(String filePath, String sourceCode) {
			super();
			this.filePath = filePath;
			this.sourceCode = sourceCode;
		}
	}

	public static class TargetClassDescriptor {
		public final String filePath;
		public final String className;

		public TargetClassDescriptor(String filePath, String className) {
			super();
			this.filePath = filePath;
			this.className = className.replace('/', '.');
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof TargetClassDescriptor) {
				TargetClassDescriptor tcd = (TargetClassDescriptor) obj;
				return filePath.equals(tcd.filePath) && className.equals(tcd.className);
			}
			return false;

		}

		@Override
		public int hashCode() {
			return className.hashCode() + filePath.hashCode() * 100000;
		}
	}

	public static class TargetClassLoader extends ClassLoader {

		private Map<TargetClassDescriptor, MemoryByteCode> classes = new HashMap<TargetClassDescriptor, MemoryByteCode>();

		@Override
		public InputStream getResourceAsStream(String name) {
			if (name.endsWith(".class"))
				name = name.substring(0, name.length() - ".class".length());
			for (TargetClassDescriptor tcd : classes.keySet()) {
				if (tcd.className.equals(name.replace('/', '.'))) {

					ByteArrayInputStream bais = new ByteArrayInputStream(classes.get(tcd).getBytes());
					return bais;
				}
			}
			return super.getResourceAsStream(name);
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			MemoryByteCode mbc = null; // classes.get(name);
			for (TargetClassDescriptor tcd : classes.keySet()) {
				if (tcd.className.equals(name.replace('/', '.'))) {
					mbc = classes.get(tcd);
					break;
				}
			}

			if (mbc == null) {
				return super.findClass(name);
			}

			return defineClass(name.replace('/', '.'), mbc.getBytes(), 0, mbc.getBytes().length);
		}

		public TargetClassLoader(ClassLoader parent) {
			super(parent);
		}

		public void addClass(TargetClassDescriptor key, MemoryByteCode mbc) {
			classes.put(key, mbc);
		}

		public boolean containsKey(Object key) {
			return classes.containsKey(key);
		}

		public boolean isEmpty() {
			return classes.isEmpty();
		}

		public Set<TargetClassDescriptor> nameSet() {
			return classes.keySet();
		}

		public int size() {
			return classes.size();
		}

	}

	public static TargetClassLoader compile(ClassLoader baseClassLoader, Collection<SourceInMemory> sourceClasses) {
		try {
			TargetClassLoader cl = new TargetClassLoader(baseClassLoader);
			if (sourceClasses.size() == 0)
				return cl;

			JavaCompiler javac = new EclipseCompiler();

			StandardJavaFileManager sjfm = javac.getStandardFileManager(null, null, null);
			SpecialJavaFileManager fileManager = new SpecialJavaFileManager(sjfm, cl);
			List<String> options = Collections.emptyList();

			List<MemorySource> mss = new ArrayList<MemorySource>();
			for (SourceInMemory scls : sourceClasses) {
				mss.add(new MemorySource(scls.filePath, scls.sourceCode));
			}

			List<MemorySource> compilationUnits = mss;
			DiagnosticListener<? super JavaFileObject> dianosticListener = new DiagnosticListener<JavaFileObject>() {

				@Override
				public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
					String pos = /* diagnostic.getSource().getName() + */ " at line " + diagnostic.getLineNumber()
							+ ", col " + diagnostic.getColumnNumber() + ": " + diagnostic.getMessage(Locale.ENGLISH);

					if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
						logger.error("Compilation error in " + pos);
					} else if (diagnostic.getKind() == Diagnostic.Kind.WARNING) {
						logger.warn("Compilation warning in " + pos);
					} else if (diagnostic.getKind() == Diagnostic.Kind.MANDATORY_WARNING) {
						logger.warn("Compilation warning in " + pos);
					} else if (diagnostic.getKind() == Diagnostic.Kind.NOTE) {
						logger.info("Compilation note in " + pos);
					}
				}
			};

			Iterable<String> classes = null;
			Writer out = new PrintWriter(System.err);

			JavaCompiler.CompilationTask compile = javac.getTask(out, fileManager, dianosticListener, options, classes,
					compilationUnits);
			boolean success = compile.call();
			if (success) {
				return cl;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}

class MemorySource extends SimpleJavaFileObject {
	private String src;

	public MemorySource(String name, String src) {
		super(URI.create("file:///" + name), Kind.SOURCE);
		this.src = src;
	}

	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		return src;
	}

	public OutputStream openOutputStream() {
		throw new IllegalStateException();
	}

	public InputStream openInputStream() {
		return new ByteArrayInputStream(src.getBytes());
	}
}

class SpecialJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
	private JavaCompilerTool.TargetClassLoader xcl;

	public SpecialJavaFileManager(StandardJavaFileManager sjfm, JavaCompilerTool.TargetClassLoader xcl) {
		super(sjfm);
		this.xcl = xcl;
	}

	public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind,
			FileObject sibling) throws IOException {
		MemoryByteCode mbc = new MemoryByteCode(className);
		String filePath = sibling.getName();
		xcl.addClass(new JavaCompilerTool.TargetClassDescriptor(filePath, className), mbc);
		return mbc;
	}

	public ClassLoader getClassLoader(Location location) {
		return xcl;
	}
}

class MemoryByteCode extends SimpleJavaFileObject {
	private ByteArrayOutputStream baos;

	public MemoryByteCode(String name) {
		super(URI.create("byte:///" + name + ".class"), Kind.CLASS);
	}

	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		throw new IllegalStateException();
	}

	public OutputStream openOutputStream() {
		baos = new ByteArrayOutputStream();
		return baos;
	}

	public InputStream openInputStream() {
		throw new IllegalStateException();
	}

	public byte[] getBytes() {
		return baos.toByteArray();
	}
}
