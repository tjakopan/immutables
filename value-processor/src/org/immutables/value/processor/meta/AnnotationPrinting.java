/*
    Copyright 2014 Ievgen Lukash

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.value.processor.meta;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor7;
import org.immutables.generator.StringLiterals;

/**
 * The gymnastics that exists only to workaround annotation processors that do not fully print
 * annotation values (like Eclipse compiler)
 */
final class AnnotationPrinting {
  private static final String PREFIX_JAVA_LANG = "@java.lang.";
  private static final String PREFIX_ORG_IMMUTABLES = "@org.immutables.";

  static CharSequence toCharSequence(AnnotationValue value) {
    PrintVisitor printer = new PrintVisitor();
    printer.visitValue(value);
    return printer.builder;
  }

  static List<CharSequence> getAnnotationLines(ExecutableElement element) {
    List<CharSequence> lines = Lists.newArrayList();

    for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
      String string = annotation.toString();
      if (string.startsWith(PREFIX_ORG_IMMUTABLES) || string.startsWith(PREFIX_JAVA_LANG)) {
        continue;
      }
      PrintVisitor printer = new PrintVisitor();
      printer.visitAnnotation(annotation, null);
      lines.add(printer.builder);
    }

    return lines;
  }

  private static final class PrintVisitor extends SimpleAnnotationValueVisitor7<Void, Void> {
    final StringBuilder builder = new StringBuilder();

    void visitValue(AnnotationValue value) {
      value.accept(this, null);
    }

    @Override
    protected Void defaultAction(Object o, Void p) {
      builder.append(o);
      return null;
    }

    @Override
    public Void visitString(String s, Void p) {
      builder.append(StringLiterals.toLiteral(s));
      return null;
    }

    @Override
    public Void visitType(TypeMirror t, Void p) {
      builder.append(t).append(".class");
      return null;
    }

    @Override
    public Void visitArray(List<? extends AnnotationValue> vals, Void p) {
      builder.append('{');
      boolean notFirst = false;
      for (AnnotationValue v : vals) {
        if (notFirst) {
          builder.append(',');
        }
        notFirst = true;
        visitValue(v);
      }
      builder.append('}');
      return null;
    }

    @Override
    public Void visitEnumConstant(VariableElement c, Void p) {
      builder.append(c.getEnclosingElement()).append('.').append(c.getSimpleName());
      return null;
    }

    @Override
    public Void visitAnnotation(AnnotationMirror a, Void p) {
      String annotationString = a.toString();

      int startOfParameters = annotationString.indexOf('(');
      if (startOfParameters > 0) {
        annotationString = annotationString.substring(0, startOfParameters);
      }

      builder.append(annotationString);

      Map<? extends ExecutableElement, ? extends AnnotationValue> values = a.getElementValues();
      if (!values.isEmpty()) {
        builder.append('(');
        boolean notFirst = false;
        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : values.entrySet()) {
          if (notFirst) {
            builder.append(',');
          }
          notFirst = true;
          builder.append(e.getKey().getSimpleName()).append(" = ");
          visitValue(e.getValue());
        }
        builder.append(')');
      }
      return null;
    }
  }
}
