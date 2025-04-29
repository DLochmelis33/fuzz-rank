import os
import subprocess
import shutil

JAZZER_HOME = os.environ['JAZZ_HOME']
JACOCO_HOME = os.environ['JACOCO_HOME']


def run_jazzer(
        *,
        cp: list[str], 
        autofuzz_target: str, 
        run_workdir: str,
        time_limit: int,
):
    cp_str = ';'.join(cp) # + PATH_SEP + f'{JAZZER_HOME}/jazzer_standalone.jar'
    if not os.path.exists(run_workdir):
        os.makedirs(run_workdir)
    
    command = [
        'java',
        # JVM arguments
        f'-javaagent:{JACOCO_HOME}/jacocoagent.jar=excludes=com.code_intelligence.jazzer.*',
        '-cp',
        f'{JAZZER_HOME}/jazzer_standalone.jar;{cp_str}',
        'com.code_intelligence.jazzer.Jazzer',
        # Jazzer arguments
        f'--autofuzz={autofuzz_target}',
        '--autofuzz_ignore=java.lang.NullPointerException', # maybe not?
        f'-max_total_time={time_limit}',
        '--keep_going=0',
    ]
    print(command)

    subprocess.run(
        args=command,
        stdout=open(f'{run_workdir}/stdout.txt', 'w'),
        stderr=open(f'{run_workdir}/stderr.txt', 'w'),
        cwd=run_workdir,
    )


if __name__=="__main__":
    cp = [
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
    target = "org.assertj.vavr.api.SeqAssert::isSorted()" # "org.assertj.vavr.api.VavrAssumptions::asAssumption(org.assertj.vavr.api.AbstractVavrAssert)"
    
    workdir = 'workdir'
    if os.path.exists(workdir):
        shutil.rmtree(workdir)
    
    run_jazzer(
        cp=cp, 
        autofuzz_target=target, 
        run_workdir=workdir,
    )
