# Open Source Project Template

This repository serves as a template for Pinterest's open source projects. It
contains the canonical copies of common files for licensing, contribution,
etc.

- [`ADOPTERS.md`](ADOPTERS.md) - list of project adopters
- [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md) - code of conduct
- [`CONTRIBUTING.md`](CONTRIBUTING.md) - contributing guide
- [`LICENSE`](LICENSE) - our standard [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0)
- [`SECURITY.md`](SECURITY.md) - security policy

## Licensing

In addition to including the [`LICENSE`](LICENSE) file in the root of your
repository, you should also add the following comment header to *all* of your
project's source files (using the current year in place of `[yyyy]`):

    Copyright [yyyy]-present, Pinterest, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

For the purposes of licensing, all significant project files are considered
"source files". However, it doesn't make sense to add license headers to files
such as:

- Short information text files (`README`, etc.)
- Test data that would become malformed with the addition of the license text

Large projects may also use a shorter per-file copyright header, such as:

    /**
     * Copyright [yyyy]-present, Pinterest, Inc.
     *
     * This source code is licensed under the Apache License, Version 2.0
     * found in the LICENSE file in the root directory of this source tree.
     */

Lastly, please ensure that the `LICENSE` file is included as part of all
release packages and distribution archives, including things like JAR files.

## Adopters

You can also include an [`ADOPTERS.md`](ADOPTERS.md) file to list people and
organizations who are using the project. This can be a good way to demonstrate
a project's popularity.

In general, it's best for adopters to add themselves to the list via a Pull
Request, but project maintainers can also add to the list on their behalf
if they receive permission.

You can also add links to sites such as [AppSight][] and [StackShare][] if you
think the information there is accurate.

[AppSight]: https://www.appsight.io/
[StackShare]: https://stackshare.io/

## Contributions

It's helpful to add a [`CONTRIBUTING.md`](CONTRIBUTING.md) file to help
potential contributors understand how the project works. This is a good place
to describe the contribution process, style guide, and testing requirements.

The template provided here is a good starting point, but definitely customize
it for your project. Check out [`elixir-thrift/CONTRIBUTING.md`][et-contrib]
as an example.

[et-contrib]: https://github.com/pinterest/elixir-thrift/blob/master/CONTRIBUTING.md

## Code of Conduct

You should include a copy of [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md) in the
root of your project.

## Issues

Adding an issue template to `.github/ISSUE_TEMPLATE.md` directory is a good idea
to reduce the number of non-actionable issues the project receives.
