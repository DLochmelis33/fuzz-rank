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
    
    workdir = 'workdir/tests'
    if os.path.exists(workdir):
        shutil.rmtree(workdir)
    
    single_autofuzz(
        cp=_cp, 
        autofuzz_target=target, 
        run_workdir=workdir,
        time_per_target_seconds=10,
    )
    

def test_run_one_project():
    rankings_file = '../tmp/rankings/assertj-vavr-cd521160aa.json'
    
    workdir = str(Path('workdir/tests').absolute())
    if os.path.exists(workdir):
        shutil.rmtree(workdir)
    
    run_one_project(rankings_file, workdir, 4, 2)


def test_run_dataset():
    print("don't forget to set mock dataset envvar!")
    rankings_dir = '../tmp/rankings'
    workdir = str(Path('workdir/tests').absolute())
    if os.path.exists(workdir):
        shutil.rmtree(workdir)
        
    run_dataset(rankings_dir, workdir, 8, 10)


if __name__=="__main__":
    # test_single_autofuzz()
    test_run_one_project()
    # test_run_dataset()
