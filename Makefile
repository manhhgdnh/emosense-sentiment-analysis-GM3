JAVAC := javac
JAVA := java
MAIN_SRC := $(shell find src/main/java -name '*.java')
TEST_SRC := $(shell find src/test/java -name '*.java')
BUILD_DIR := build/classes
TEST_BUILD_DIR := build/test-classes

.PHONY: all compile run demo test figures clean

all: compile

compile:
	mkdir -p $(BUILD_DIR)
	$(JAVAC) -encoding UTF-8 -d $(BUILD_DIR) $(MAIN_SRC)

run: compile
	$(JAVA) -Djava.awt.headless=true -cp $(BUILD_DIR) app.EmoSenseApp

demo: compile
	$(JAVA) -Djava.awt.headless=true -cp $(BUILD_DIR) app.RuleBasedDemo

figures: demo
	python3 scripts/generate_figures.py

test: compile
	mkdir -p $(TEST_BUILD_DIR)
	$(JAVAC) -encoding UTF-8 -cp $(BUILD_DIR) -d $(TEST_BUILD_DIR) $(TEST_SRC)
	$(JAVA) -Djava.awt.headless=true -cp $(BUILD_DIR):$(TEST_BUILD_DIR) test.RuleBasedSmokeTest

clean:
	rm -rf build
