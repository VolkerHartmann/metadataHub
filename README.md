# MetadataHub

Acts as a facade for multiple instances of a metadata repository.
The server implements the Digital Object Interface Protocol specifier by DONA (DOIP).
For adding a new repository some code might be necessary.
1. New repositories have to be added via configuration.
2. Add code for transforming datacite metadata used by DOIP into proprietary metadata used by repository and vice versa.
3. Add code for serving repository
    - In case of an existing REST interface some Configuration might be sufficient.

# Documentation
ToDo

## API provided by DOIP
See [Turntable API](https://volkerhartmann.github.io/turntable/) for further details.

## How to add a new repository
ToDo

# Links
- [DOIP Specification] (https://www.dona.net/sites/default/files/2018-11/DOIPv2Spec_1.pdf)
- [DOIP SDK] (https://www.dona.net/sites/default/files/2020-09/DOIPv2SDKOverview.pdf)
- [Turntable API](https://volkerhartmann.github.io/turntable/)