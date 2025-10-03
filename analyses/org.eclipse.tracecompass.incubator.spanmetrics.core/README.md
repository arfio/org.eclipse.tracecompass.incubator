# Trace collection for SpanMetric patch

HotROD (Rides on Demand) is a demo application that consists of several microservices and illustrates the use of the OpenTracing API.  
It can be run standalone, but requires Jaeger's backend to view the traces.
For more information take a look at [OpenTracing for a HotROD ride](https://medium.com/opentracing/take-opentracing-for-a-hotrod-ride-f6e3141f7941).

## Prerequisites
The HotROD application is implemented in Go and requires a Go toolchain installation [Download and install](https://go.dev/doc/install).

```
sudo add-apt-repository ppa:longsleep/golang-backports
sudo apt install golang-go
echo export GOPATH="$HOME/go" >> ~/.bashrc
Unset GOROOT
```
Run background docker jaeger by the following command [Read more](https://medium.com/velotio-perspectives/a-comprehensive-tutorial-to-implementing-opentracing-with-jaeger-a01752e1a8ce).

```
sudo docker run \
  --rm \
  --name jaeger \
  -p6831:6831/udp \
  -p16686:16686 \
  jaegertracing/all-in-one:latest

http://localhost:16686/search
```
Run HotROD from the source
```
git clone git@github.com:jaegertracing/jaeger.git jaeger
cd jaeger
go run ./examples/hotrod/main.go all
```
While you are in the jaeger folder from the previous step,
get the latest version of the source code of the jaeger in the example source folder of the HotROD application [Read more](https://github.com/jaegertracing/jaeger/tree/main/examples/hotrod).

```
cd examples/hotrod
git clone https://github.com/jaegertracing/jaeger-client-go.git
```
The latest version of jaeger-client-go does not contain the go.mod so we need to follow the following steps in the jaeger folder.

```
git checkout v2.30.0
git revert f188b2ed04b88766c82731c2f923e681eec5a32d
```
Now you have go.mod file in the jeager-client-go folder.

```
cd jaeger-client-go
vim tracer.go
```
Add the followng header to tracer.go file. Instrumenting the source code of the jaeger was initialted form the previous work at Dorsal Lab [Article's Link](https://www.mdpi.com/2079-9292/10/21/2610).
The source code is available at [Loic Gelle's github](https://github.com/loicgelle/jaeger-go-lttng-instr).

```
	lttng "github.com/loicgelle/jaeger-go-lttng-instr"
```
Change the resportSpan method in the tracer.go file by adding the following code:

```
	lttng.ReportEndSpan(
		uint64(sp.context.traceID.High),
		uint64(sp.context.traceID.Low),
		uint64(sp.context.spanID),
		sp.duration,
	)
```

Change the StartSpan method in the tracer.go file by adding the followng code:

```
	lttng.ReportStartSpan(
		uint64(ctx.traceID.High),
		uint64(ctx.traceID.Low),
		uint64(ctx.spanID),
		uint64(ctx.parentID),
		operationName,
		options.StartTime,
	)
```
Go to the go.mod of the HotROD example.


```
go mod edit -replace github.com/uber/jaeger-client-go= [absolute directory path of your jaeger-client-go source code]
go mod tidy
go run ./examples/hotrod/main.go all
```

## How to collect data
```
sudo lttng create hotrod --output="[your absolute directory]"
sudo lttng enable-channel --userspace userchannel
sudo lttng enable-channel --kernel kernelchannel
sudo lttng enable-event -c kernelchannel -k -a
sudo lttng enable-event -c userchannel --userspace -a
sudo lttng add-context --userspace -t vtid -t vpid
sudo lttng add-context --kernel -t tid -t pid
sudo lttng start
sudo lttng stop
```

