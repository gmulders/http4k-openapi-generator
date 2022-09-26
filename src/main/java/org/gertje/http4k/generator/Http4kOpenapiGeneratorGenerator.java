package org.gertje.http4k.generator;

import com.google.common.collect.Maps;
import org.openapitools.codegen.*;
import org.openapitools.codegen.languages.AbstractKotlinCodegen;
import org.openapitools.codegen.model.*;
import io.swagger.models.properties.*;
import org.openapitools.codegen.templating.mustache.CamelCaseLambda;

import java.util.*;
import java.io.File;
import java.util.stream.Collectors;

import static org.openapitools.codegen.utils.StringUtils.camelize;

public class Http4kOpenapiGeneratorGenerator extends AbstractKotlinCodegen implements CodegenConfig {

  public static final String LENS_SPEC_IMPORT = "lensSpecImport";

  // source folder where to write the files
  protected String sourceFolder = "src/main/kotlin";
  protected String apiVersion = "1.0.0";

  protected String lensSpecImport = "org.http4k.format.Jackson.auto";

  /**
   * Configures the type of generator.
   *
   * @return the CodegenType for this generator
   * @see org.openapitools.codegen.CodegenType
   */
  public CodegenType getTag() {
    return CodegenType.OTHER;
  }

  /**
   * Configures a friendly name for the generator.  This will be used by the generator
   * to select the library with the -g flag.
   *
   * @return the friendly name for the generator
   */
  public String getName() {
    return "http4k-openapi-generator";
  }

  /**
   * Provides an opportunity to inspect and modify operation data before the code is generated.
   */
  @Override
  public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {

    // to try debugging your code generator:
    // set a break point on the next line.
    // then debug the JUnit test called LaunchGeneratorInDebugger

    List<Map<String, String>> imports = objs.getImports();

    imports.add(Collections.singletonMap("import", "org.http4k.routing.RoutingHttpHandler"));

    OperationsMap results = super.postProcessOperationsWithModels(objs, allModels);

    OperationMap ops = results.getOperations();
    List<CodegenOperation> opList = ops.getOperation();

    List<Map<String, String>> bodyLenses = new ArrayList<>();
    List<Map<String, String>> otherLenses = new ArrayList<>();
    Set<String> lensImports = new HashSet<>();

    Map<String, String> lenses = new HashMap<>();
    // iterate over the operation and perhaps modify something
    for (CodegenOperation co : opList) {
      for (CodegenParameter param : co.allParams) {

        if (!param.isBodyParam) {
          lenses.put(param.baseName + camelize(param.dataFormat) + lensType(param) + "Lens", lensType(param) + "." + lensMap(param.dataFormat) + "()." + lensMethod(param.isPathParam, param.required) + "(\"" + param.baseName + "\")");
          lensImports.add("org.http4k.lens." + lensType(param));
          lensImports.add("org.http4k.lens." + lensMap(param.dataFormat));
        } else {
          lenses.put(param.paramName + lensType(param) + "Lens", lensType(param) + ".auto<" + param.dataType + ">().toLens()");

          lensImports.add("org.http4k.core.Body");
          lensImports.add("org.http4k.format.Jackson.auto");
        }
      }
    }

    for (CodegenOperation co : opList) {
      for (CodegenProperty param : co.responseHeaders) {
        lenses.put(camelize(param.baseName, true) + camelize(param.dataFormat) + "HeaderLens", "Header." + lensMap(param.dataFormat) + "()." + lensMethod(false, param.required) + "(\"" + param.baseName + "\")");
        lensImports.add("org.http4k.lens.Header");
        lensImports.add("org.http4k.lens." + lensMap(param.dataFormat));
      }
    }
    // Onderstaande alleen toevoegen wanneer we een body uitlezen ergens. Hoe weet ik dat?

    lenses.forEach((key, value) -> {
      Map<String, String> lens = new HashMap<>();
      lens.put("name", key);
      lens.put("definition", value);
      otherLenses.add(lens);
    });

    lensImports.forEach((value) -> {
      imports.add(Collections.singletonMap("import", value));
    });
    objs.setImports(imports.stream().sorted(Comparator.comparing((a) -> a.get("import"))).collect(Collectors.toList()));
    ops.put("bodyLenses", bodyLenses);
    ops.put("otherLenses", otherLenses);

    return results;
  }

  private static String lensType(CodegenParameter param) {
    if (param.isQueryParam) {
      return "Query";
    } else if (param.isPathParam) {
      return "Path";
    } else if (param.isHeaderParam) {
      return "Header";
    } else if (param.isCookieParam) {
      return "Cookies";
    } else if (param.isBodyParam) {
      return "Body";
    }
    return "<not found>";
  }

  private static String lensMethod(boolean isPathParam, boolean required) {
    if (isPathParam) {
      return "of";
    } else if (required) {
      return "required";
    } else {
      return "optional";
    }
  }

  private static String lensMap(String dataFormat) {
    switch (dataFormat) {
      case "uuid": return "uuid";
      case "int32":
      case "int64":
        return "int";
    }
    return "<no lens map found>";
  }

  /**
   * Returns human-friendly help for the generator.  Provide the consumer with help
   * tips, parameters here
   *
   * @return A string value for the help message
   */
  public String getHelp() {
    return "Generates a http4k client library.";
  }

  public Http4kOpenapiGeneratorGenerator() {
    super();

    // set the output folder here
    outputFolder = "generated-code/http4k-openapi-generator";

    /**
     * Models.  You can write model files using the modelTemplateFiles map.
     * if you want to create one template for file, you can do so here.
     * for multiple files for model, just put another entry in the `modelTemplateFiles` with
     * a different extension
     */
    modelTemplateFiles.put("model.mustache", ".kt");

    /**
     * Api classes.  You can write classes for each Api file with the apiTemplateFiles map.
     * as with models, add multiple entries with different extensions for multiple files per
     * class
     */
    apiTemplateFiles.put("api.mustache", ".kt");

    apiDocTemplateFiles.put("api_doc.mustache", ".md");

    /**
     * Template Location.  This is the location which templates will be read from.  The generator
     * will use the resource stream to attempt to read the templates.
     */
    templateDir = "http4k-openapi-generator";

    /**
     * Api Package.  Optional, if needed, this can be used in templates
     */
    apiPackage = "org.openapitools.api";

    /**
     * Model Package.  Optional, if needed, this can be used in templates
     */
    modelPackage = "org.openapitools.model";

    /**
     * Reserved words.  Override this with reserved words specific to your language
     */
    reservedWords = new HashSet<String>(
            Arrays.asList(
                    "sample1",  // replace with static values
                    "sample2")
    );

    /**
     * Additional Properties.  These values can be passed to the templates and
     * are available in models, apis, and supporting files
     */
    additionalProperties.put("apiVersion", apiVersion);
    additionalProperties.put("camelcase", new CamelCaseLambda());

    /**
     * Supporting Files.  You can write single files for the generator with the
     * entire object tree available.  If the input file has a suffix of `.mustache
     * it will be processed by the template engine.  Otherwise, it will be copied
     */
    supportingFiles.add(new SupportingFile("myFile.mustache", "", "myFile.sample"));

    cliOptions.add(CliOption.newString(LENS_SPEC_IMPORT, "").defaultValue(""));

  }

  @Override
  public void processOpts() {
    super.processOpts();

    if (additionalProperties.containsKey(LENS_SPEC_IMPORT)) {
      lensSpecImport = additionalProperties.get(LENS_SPEC_IMPORT).toString();
    }
  }

  /**
   * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
   * those terms here.  This logic is only called if a variable matches the reserved words
   *
   * @return the escaped term
   */
  @Override
  public String escapeReservedWord(String name) {
    return "_" + name;  // add an underscore to the name
  }

  /**
   * Location to write model files.  You can use the modelPackage() as defined when the class is
   * instantiated
   */
  public String modelFileFolder() {
    return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', File.separatorChar);
  }

  /**
   * Location to write api files.  You can use the apiPackage() as defined when the class is
   * instantiated
   */
  @Override
  public String apiFileFolder() {
    return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.', File.separatorChar);
  }

  /**
   * override with any special text escaping logic to handle unsafe
   * characters so as to avoid code injection
   *
   * @param input String to be cleaned up
   * @return string with unsafe characters removed or escaped
   */
  @Override
  public String escapeUnsafeCharacters(String input) {
    //TODO: check that this logic is safe to escape unsafe characters to avoid code injection
    return input;
  }

  /**
   * Escape single and/or double quote to avoid code injection
   *
   * @param input String to be cleaned up
   * @return string with quotation mark removed or escaped
   */
  public String escapeQuotationMark(String input) {
    //TODO: check that this logic is safe to escape quotation mark to avoid code injection
    return input.replace("\"", "\\\"");
  }
}
