.DEV_PROFILE := okdata-dev
.PROD_PROFILE := okdata-prod


.PHONY: init
init:
	npm install

.PHONY: clean
clean:
	./gradlew clean

.PHONY: test
test:
	./gradlew check

.PHONY: format
format:
	./gradlew ktlintformat

.PHONY: build
build:
	./gradlew shadowJar

.PHONY: deploy
deploy: init test build login-dev
	@echo "\nDeploying to stage: dev\n"
	sls deploy --stage dev --aws-profile $(.DEV_PROFILE)

.PHONY: deploy-prod
deploy-prod: init is-git-clean test build login-prod
	sls deploy --stage prod --aws-profile $(.PROD_PROFILE)

.PHONY: undeploy
undeploy: login-dev init
	@echo "\nUndeploying stage: dev\n"
	sls remove --stage dev --aws-profile $(.DEV_PROFILE)

.PHONY: undeploy-prod
undeploy-prod: login-prod init
	@echo "\nUndeploying stage: prod\n"
	sls remove --stage prod --aws-profile $(.PROD_PROFILE)

.PHONY: login-dev
login-dev: init
	aws sts get-caller-identity --profile $(.DEV_PROFILE) || aws sso login --profile=$(.DEV_PROFILE)

.PHONY: login-prod
login-prod: init
	aws sts get-caller-identity --profile $(.PROD_PROFILE) || aws sso login --profile=$(.PROD_PROFILE)

.PHONY: is-git-clean
is-git-clean:
	@status=$$(git fetch origin && git status -s -b) ;\
	if test "$${status}" != "## master...origin/master"; then \
		echo; \
		echo Git working directory is dirty, aborting >&2; \
		false; \
	fi
