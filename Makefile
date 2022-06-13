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
deploy: login-dev init format test
	@echo "\nDeploying to stage: dev\n"
	sls deploy --stage dev --aws-profile $(.DEV_PROFILE)

.PHONY: deploy-prod
deploy-prod: login-prod init format is-git-clean test
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
	./node_modules/.bin/ssocreds -p $(.DEV_PROFILE) # https://github.com/serverless/serverless/issues/7567

.PHONY: login-prod
login-prod: init
	aws sts get-caller-identity --profile $(.PROD_PROFILE) || aws sso login --profile=$(.PROD_PROFILE)
	./node_modules/.bin/ssocreds -p $(.PROD_PROFILE) # https://github.com/serverless/serverless/issues/7567

.PHONY: is-git-clean
is-git-clean:
	@status=$$(git fetch origin && git status -s -b) ;\
	if test "$${status}" != "## master...origin/master"; then \
		echo; \
		echo Git working directory is dirty, aborting >&2; \
		false; \
	fi
