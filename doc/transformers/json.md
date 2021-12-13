JSON to JSON transformer
========================

Pipeline component for transforming JSON using [JSLT](https://github.com/schibsted/jslt).

## Input event

The input event contains the input location, output destination and the CSVLT expression. Input and output can either be of type "S3" and contain a s3-key or it can be of type "INLINE" and contain the json object directly.

Example Lambda event input:

```json
{
  "input": {
    "type": "S3",
    "value": "input_s3_key.json"
  },
  "output": {
    "type": "S3",
    "value": "output_s3_key.json"
  },
  "config": {
      "jslt": "let idparts = split(.id, \"-\")\nlet xxx = [for ($idparts) \"x\" * size(.)]\n\n{\n  \"id\" : join($xxx, \"-\"),\n  \"type\" : \"Anonymized-View\",\n  * : .\n}\n"
  }
}
```

The input event object takes the following key/values:

| Key               | Type      | Description                                             | Default value                     |
| ----------------- | --------- | -------------------------------------------------       | --------------------------------- |
| input             | Object    | Type('type') and location('value') for input json       | <no default, must be supplied>    |
| output            | Object    | Type('type') and destination('value') for output json   | <no default, must be supplied>    |
| jslt              | String    | The JSLT expression to apply                            | "{* : .}"                         |


## Output/result

The output/result contains the location of the transformed json or the transformed json directly.

Example output:

S3:
```json
{
  "value": "output_s3_key.json",
  "type": "S3"
}
```
Inline:
```json
{
  "value": "{\"id\":\"xxx\",\"type\":\"Anonymized-View\"}",
  "type": "INLINE"
}
```
