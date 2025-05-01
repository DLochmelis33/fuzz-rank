import os
import shutil
import pathlib

from run_jazzer import *
from run_jacoco import *
from run_exp import *

_cp = [
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\classes",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\apiguardian-api-1.1.2.jar",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\assertj-core-3.24.1.jar",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\byte-buddy-1.12.21.jar",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\byte-buddy-agent-1.12.9.jar",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\hamcrest-core-1.3.jar",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\junit-4.13.2.jar",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\junit-jupiter-5.9.1.jar",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\junit-jupiter-api-5.9.1.jar",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\junit-jupiter-engine-5.9.1.jar",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\junit-jupiter-params-5.9.1.jar",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\junit-platform-commons-1.9.1.jar",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\junit-platform-engine-1.9.1.jar",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\mockito-core-4.5.1.jar",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\objenesis-3.2.jar",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\opentest4j-1.2.0.jar",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\vavr-0.10.4.jar",
    "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\dependency\\vavr-match-0.10.4.jar"
]

def test_single_autofuzz():
    target = "org.assertj.vavr.api.SeqAssert::isSorted()" # "org.assertj.vavr.api.VavrAssumptions::asAssumption(org.assertj.vavr.api.AbstractVavrAssert)"
    
    workdir = 'workdir'
    if os.path.exists(workdir):
        shutil.rmtree(workdir)
    
    single_autofuzz(
        cp=_cp, 
        autofuzz_target=target, 
        run_workdir=workdir,
        time_limit_seconds=10,
    )
    

def test_parallel_autofuzz():
    targets = [
        "org.assertj.vavr.api.VavrAssumptions::asAssumption(org.assertj.vavr.api.AbstractVavrAssert)",
        "org.assertj.vavr.api.AbstractMultimapAssert::hasSameSizeAs(java.lang.Iterable)",
        "org.assertj.vavr.api.ClassLoadingStrategyFactory::classLoadingStrategy(java.lang.Class)",
        "org.assertj.vavr.api.AbstractOptionAssert::contains(java.lang.Object)",
        "org.assertj.vavr.api.AbstractTryAssert::contains(java.lang.Object)",
        "org.assertj.vavr.api.VavrAssumptions::asAssumption(java.lang.Class, java.lang.Class[], java.lang.Object[])",
        "org.assertj.vavr.api.VavrAssumptions.AssumptionMethodInterceptor::intercept(org.assertj.vavr.api.AbstractVavrAssert, java.util.concurrent.Callable)",
        "org.assertj.vavr.api.AbstractSeqAssert::isSorted()",
        "org.assertj.vavr.api.AbstractSeqAssert::assertIsSortedAccordingToComparator(java.util.Comparator)",
        "org.assertj.vavr.api.VavrAssumptions::assumptionNotMet(java.lang.AssertionError)",
        "org.assertj.vavr.api.AbstractValidationAssert::containsInvalid(java.lang.Object)",
        "org.assertj.vavr.api.AbstractMapAssert::hasSizeBetween(int, int)",
        "org.assertj.vavr.api.AbstractOptionAssert::containsSame(java.lang.Object)",
        "org.assertj.vavr.api.AbstractSeqAssert::contains(java.lang.Object, org.assertj.core.data.Index)",
        "org.assertj.vavr.api.AbstractEitherAssert::containsOnLeft(java.lang.Object)",
        "org.assertj.vavr.api.AbstractEitherAssert::containsOnRight(java.lang.Object)"
    ]
    # 16 targets
    # 4 in parallel, 10 seconds per each = 40 seconds total
    
    workdir = 'workdir'
    if os.path.exists(workdir):
        shutil.rmtree(workdir)
        
    parallel_autofuzz(
        cp=_cp,
        targets=targets,
        workdir=workdir,
        parallelism=4,
        time_per_ranking_seconds=40
    )    
    

def test_jacoco_merge():
    workdir = pathlib.Path('workdir')
    exec_files = [
        workdir / d / 'jacoco.exec'
        for d in os.listdir(workdir)]
    output = workdir / 'jacoco_merged.exec'
    jacoco_merge(exec_files, output)


def test_jacoco_report():
    output_dir = 'workdir/report'
    os.makedirs(output_dir)
    jacoco_report(
        'workdir/jacoco_merged.exec', 
        ["C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\classes"], 
        output_dir
    )


def test_run_one_project():
    rankings_file = 'results/rankings/ari-proxy-f9fde350e2.json'
    
    workdir = str(Path('workdir').absolute())
    if os.path.exists(workdir):
        shutil.rmtree(workdir)
    
    run_one_project(rankings_file, workdir, 4, 2)


# use a mock dataset in env var and mock rankings
def test_run_dataset():
    rankings_dir = '../tmp/rankings'
    workdir = str(Path('workdir').absolute())
    if os.path.exists(workdir):
        shutil.rmtree(workdir)
        
    run_dataset(rankings_dir, workdir, 8, 60 * 10)


if __name__=="__main__":
    # test_single_autofuzz()
    # test_parallel_autofuzz()
    # test_jacoco_merge()
    # test_jacoco_report()
    # test_run_one_project()
    test_run_dataset()
